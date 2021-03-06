package com.xtao.wechat.listener;

import com.xtao.wechat.WX;
import com.xtao.wechat.callback.NewMessageCallback;
import com.xtao.wechat.constant.ApiUrl;
import com.xtao.wechat.model.Msg;
import com.xtao.wechat.util.HttpRequest;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 　 　　   へ　　　 　／|
 * 　　    /＼7　　　 ∠＿/
 * 　     /　│　　 ／　／
 * 　    │　Z ＿,＜　／　　   /`ヽ
 * 　    │　　　 　　ヽ　    /　　〉
 * 　     Y　　　　　   `　  /　　/
 * 　    ｲ●　､　●　　⊂⊃〈　　/
 * 　    ()　 へ　　　　|　＼〈
 * 　　    >ｰ ､_　 ィ　 │ ／／      去吧！
 * 　     / へ　　 /　ﾉ＜| ＼＼        比卡丘~
 * 　     ヽ_ﾉ　　(_／　 │／／           消灭代码BUG
 * 　　    7　　　　　　　|／
 * 　　    ＞―r￣￣`ｰ―＿
 * ━━━━━━感觉萌萌哒━━━━━━
 *
 * @author penghaitao
 * @date 2017/9/6  23:48
 * @description
 */
public class MessageListener extends Thread {

    private volatile boolean exit = false;

    private volatile JSONObject synckey = null;
    private NewMessageCallback messageCallback = null;

    public MessageListener(NewMessageCallback callback) {
        this.messageCallback = callback;
    }

    public void setSyncKey(JSONObject object) {
        this.synckey = object;
    }

    @Override
    public void run() {
        while (!exit) {
            try {
                int code = syncCheck();
                switch (code) {
                    case 0:
                        break;
                    case 1:
                        JSONObject response = JSONObject.fromObject(getNewMsg());
                        JSONArray msgList = response.getJSONArray("AddMsgList");
                        for (int i = 0; i < msgList.size(); ++ i) {
                            String from = msgList.getJSONObject(i).getString("FromUserName");
                            String content = msgList.getJSONObject(i).getString("Content");
                            messageCallback.onReceive(new Msg(from, content));
                            // TODO: 2017/9/8 需要刷新状态
                        }
                        break;
                    case 2:
                        messageCallback.onChat();
                        break;
                    default:
                        messageCallback.onError(code);
                        break;
                }
                Thread.sleep(20 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 消息检查返回码 格式 window.synccheck={retcode:"0",selector:"0"}
     * retcode = 0 正常；retcode = 1002 cookie失效
     * selector = 0 正常； selector = 2 新消息； selector = 7 进入/退出聊天界面
     * @return 转义消息码
     */
    private int syncCheck() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("r", String.valueOf(System.currentTimeMillis()));
        params.put("skey", WX.getInstance().sKey);
        params.put("sid", WX.getInstance().wxSid);
        params.put("uin", WX.getInstance().wxUin);
        params.put("deviceid", "e" + String.valueOf(Math.random()).substring(2));
        params.put("synckey", getSyncKeyStr());
        params.put("_", String.valueOf(++ WX.getInstance().time));
        String response = HttpRequest.get(ApiUrl.MessageCheck.getUrl(), params);
        JSONObject result = JSONObject.fromObject(response.substring(response.indexOf('{')));
        if (!result.getString("retcode").equals("0"))
            return Integer.valueOf(result.getString("retcode"));
        else if (result.getString("selector").equals("0"))
            return 0;
        else if (result.getString("selector").equals("2"))
            return 1;
        else if (result.getString("selector").equals("7"))
            return 2;
        return -1;
    }

    /**
     * 拼接同步码
     * @return 拼接的SyncKey字符串
     */
    private String getSyncKeyStr() {
        StringBuilder sb = new StringBuilder();
        JSONArray array = synckey.getJSONArray("List");
        for (int i = 0; i < array.size(); ++ i)
            sb.append("%7C").append(array.getJSONObject(i).getInt("Key")).append("_").append(array.getJSONObject(i).getInt("Val"));
        return sb.toString().substring(3);
    }

    /**
     * 获取新消息
     */
    private String getNewMsg() {
        String url = ApiUrl.NEW_MESSAGE.getUrl() +
                "？sid=" + WX.getInstance().wxSid +
                "&skey=" + WX.getInstance().sKey +
                "&pass_ticket=" + WX.getInstance().passTicket;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Uin", WX.getInstance().wxUin);
        jsonObject.put("Sid", WX.getInstance().wxSid);
        jsonObject.put("Skey", WX.getInstance().sKey);
        jsonObject.put("DeviceID", "e" + String.valueOf(Math.random()).substring(2));

        JSONObject param = new JSONObject();
        param.put("BaseRequest", jsonObject);
        param.put("SyncKey", synckey);
        param.put("rr", String.valueOf(~System.currentTimeMillis()));
       return HttpRequest.post(url, param);
    }

    /*
    window.synccheck={retcode:"1102",selector:"0"}
    1102 应该为登录信息实现失效
    retcode:
    0 正常
    1100 失败/退出微信
selector:
    0 正常
    2 新的消息
    7 进入/离开聊天界面
     */

    /*
https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxsync?sid=7zpkixo841FAhQ/C&skey=@crypt_6dea96f0_e0f53525924b6ca1cf65cb45cd390858&pass_ticket=H0LFhI%252BqoV135fhb2EfYFxJNIBpagw1G7BimSrv%252Bwj9dvyf7ykkTW%252BCke27gf1kR
发送
https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxsendmsg?pass_ticket=H0LFhI%252BqoV135fhb2EfYFxJNIBpagw1G7BimSrv%252Bwj9dvyf7ykkTW%252BCke27gf1kR

刷新
https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxstatusnotify?lang=zh_CN&pass_ticket=hzO%252FJlodtOLeZ5eedHOfd%252FuBjIIAlFXqu2yojOaq%252BGaKZsqm6cuZTuPoJw1gZZ4V
{"BaseRequest":{"Uin":2822019321,"Sid":"/qmFjsoxoMoVbsrd","Skey":"@crypt_6dea96f0_7a97c2ddebf0b95bf9f1383255f88bf1"
,"DeviceID":"e311749112878683"},"Code":1,"FromUserName":"@92aa48b8cc0079ffadd8c2823325f98d","ToUserName"
:"@31c67cc1067e4ffd50373089a563f718","ClientMsgId":1504885213217}

{
"BaseResponse": {
"Ret": 0,
"ErrMsg": ""
}
,
"MsgID": "4941606131967166287"
}
    * */

}
