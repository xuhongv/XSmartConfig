package com.xuhong.xsmartconfiglib.api;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.xuhong.xsmartconfiglib.EsptouchTask;
import com.xuhong.xsmartconfiglib.IEsptouchListener;
import com.xuhong.xsmartconfiglib.IEsptouchResult;
import com.xuhong.xsmartconfiglib.IEsptouchTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class xEspTouchTask {


    private IEsp8266TouchTask mEsptouchTaskTemp;

    private IEsptouchTask mEsptouchTask;

    private Context mContext;

    //ssid
    private String ssid;

    //密码
    private String passWord;

    //配对的设备个数,默认是1
    private int deviceCount = 1;
    private int port = 8686;


    private EspWifiAdmin mWifiAdmin;

    private EspTouchTaskListener listener;

    private boolean isUDPReceive = false;
    private boolean isReceive = true;
    private boolean isUDPReceiveFail = true;

    private int timesOut;

    private Thread mThread;

    private Selector selector = null;
    private DatagramChannel channel = null;


    private xEspTouchTask(Context mContext) {
        this.mContext = mContext;
        mWifiAdmin = new EspWifiAdmin(mContext);
    }

    public static class Builder {

        private xEspTouchTask mXEspTouchTask;

        public Builder(Context mContext) {
            mXEspTouchTask = new xEspTouchTask(mContext);
        }

        /**
         * @param ssid 设置路由器名字
         * @return
         */
        public Builder setSsid(String ssid) {
            mXEspTouchTask.ssid = ssid;
            return this;
        }

        /**
         * @param passWord 设置路由器密码
         * @return
         */
        public Builder setPassWord(String passWord) {
            mXEspTouchTask.passWord = passWord;
            return this;
        }

        /**
         * @param deviceCount 设置要配对的设备个数
         * @return
         */
        public Builder setDeviceCount(int deviceCount) {
            mXEspTouchTask.deviceCount = deviceCount;
            return this;
        }


        public xEspTouchTask creat() {
            return mXEspTouchTask;
        }
    }

    //不接受自定义广播包开始配置
    public void startSmartConfig() {
        mEsptouchTaskTemp = new IEsp8266TouchTask(mWifiAdmin);
        mEsptouchTaskTemp.execute(ssid, mWifiAdmin.getWifiConnectedBssid(), passWord, Integer.toString(deviceCount));
        isUDPReceive = false;
    }

    /**
     * 设置为配网模式+接受此设备UDP信息
     *
     * @param timesOut 设置超时时间
     * @param port     设置UDP本地的端口
     */
    public void startSmartConfig(int timesOut, int port) {
        mEsptouchTaskTemp = new IEsp8266TouchTask(mWifiAdmin);
        mEsptouchTaskTemp.execute(ssid, mWifiAdmin.getWifiConnectedBssid(), passWord, Integer.toString(deviceCount));
        isUDPReceive = true;
        this.timesOut = timesOut;
        this.port = port;
    }

    //停止
    public void stopSmartConfig() {
        if (mEsptouchTask != null) {
            mEsptouchTask.interrupt();
        }
        isReceive = false;
        if (mThread != null && channel != null) {
            mThread.interrupt();
            channel.socket().disconnect();
            channel.socket().close();
        }
    }

    public void setEspTouchTaskListener(EspTouchTaskListener listener) {
        this.listener = listener;
    }

    public interface EspTouchTaskListener {

        /**
         * @param code    0：表示成功配网，接着看message的信息 ；
         *                1：为多个配网信息，还在配网中，其中message是刚刚配对成功的设备 ;
         *                2:表示配网失败;
         *                3:表示成功接受到设备的UDP信息
         *                4:表示超过了设置超时时间，未接受到设备的UDP信息
         * @param message
         */
        void EspTouchTaskCallback(int code, String message);

    }

    private class IEsp8266TouchTask extends AsyncTask<String, Void, List<IEsptouchResult>> {

        private EspWifiAdmin mWifiAdmin;

        private IEsp8266TouchTask(EspWifiAdmin mWifiAdmin) {
            this.mWifiAdmin = mWifiAdmin;
        }

        private final Object mLock = new Object();

        @Override
        protected void onPreExecute() {
            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected List<IEsptouchResult> doInBackground(String... params) {
            int taskResultCount = -1;
            synchronized (mLock) {
                String apSsid = mWifiAdmin.getWifiConnectedSsidAscii(params[0]);
                String apBssid = params[1];
                String apPassword = params[2];
                String taskResultCountStr = params[3];
                taskResultCount = Integer.parseInt(taskResultCountStr);
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, mContext);
                mEsptouchTask.setEsptouchListener(myListener);
            }
            List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
            return resultList;
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> iEsptouchResults) {

            IEsptouchResult firstResult = iEsptouchResults.get(0);
            if (!firstResult.isCancelled()) {
                String macAddress = null;
                String IPAddress = null;
                int count = 0;
                final int maxDisplayCount = 5;
                if (firstResult.isSuc()) {
                    JSONObject jsonObject = new JSONObject();
                    for (IEsptouchResult resultInList : iEsptouchResults) {
                        try {
                            jsonObject.put("macAddress", resultInList.getBssid());
                            jsonObject.put("IPAddress", resultInList.getInetAddress().getHostAddress());
                            macAddress = resultInList.getBssid();
                            IPAddress = resultInList.getInetAddress().getHostAddress();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
                    if (count < iEsptouchResults.size()) {
                        try {
                            jsonObject.put("downNum", (iEsptouchResults.size() - count));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (listener != null && deviceCount != 1) {
                        Message message = new Message();
                        message.what = 106;
                        message.obj = jsonObject.toString();
                        mHandler.sendMessage(message);
                        if (isUDPReceive) {
                            startUDPRecieve(macAddress, IPAddress);
                        }
                    }
                } else {
                    if (listener != null) {
                        mHandler.sendEmptyMessage(107);
                    }
                }
            }

        }

        private IEsptouchListener myListener = new IEsptouchListener() {

            @Override
            public void onEsptouchResultAdded(IEsptouchResult result) {
                if (listener != null) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("macAddress", result.getBssid());
                        jsonObject.put("IPAddress", result.getInetAddress().getHostAddress());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    Message message = new Message();
                    message.what = 108;
                    message.obj = jsonObject.toString();
                    mHandler.sendMessage(message);

                    if (isUDPReceive) {
                        startUDPRecieve(result.getBssid(), result.getInetAddress().getHostAddress());
                    }
                }
            }
        };
    }

    private void startUDPRecieve(final String macAddress, final String IPAddress) {

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isReceive = false;
                        if (listener != null && isUDPReceiveFail) {
                            mHandler.sendEmptyMessage(109);
                        }
                    }
                }, timesOut * 1000);

                try {
                    channel = DatagramChannel.open();
                    channel.configureBlocking(false);
                    channel.socket().setReuseAddress(false);
                    channel.socket().bind(new InetSocketAddress(port));
                    selector = Selector.open();
                    channel.register(selector, SelectionKey.OP_READ);
                } catch (IOException e) {
                    e.printStackTrace();

                }
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                while (isReceive) {
                    try {
                        if (selector == null) {
                            return;
                        }
                        int n = selector.select();
                        if (n > 0) {
                            Iterator iterator = selector.selectedKeys().iterator();
                            while (iterator.hasNext()) {
                                SelectionKey key = (SelectionKey) iterator.next();
                                iterator.remove();
                                if (key.isReadable()) {
                                    DatagramChannel dataChannel = (DatagramChannel) key.channel();
                                    byteBuffer.clear();
                                    InetSocketAddress address = (InetSocketAddress) dataChannel.receive(byteBuffer);
                                    String message = new String(byteBuffer.array(), 0, byteBuffer.position());
                                    if (address.getAddress().getHostAddress().equalsIgnoreCase(IPAddress)) {
                                        Log.e("==w", "address.getAddress().getHostAddress():" + address.getAddress().getHostAddress());
                                        Message message1 = new Message();
                                        message1.what = 105;
                                        message1.obj = message;
                                        mHandler.sendMessage(message1);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mThread.start();

    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 105) {
                if (listener != null) {
                    isReceive = false;
                    isUDPReceiveFail = false;
                    String message = (String) msg.obj;
                    listener.EspTouchTaskCallback(3, message);
                }
            } else if (msg.what == 106) {
                String message = (String) msg.obj;
                listener.EspTouchTaskCallback(1, message);
            } else if (msg.what == 107) {
                listener.EspTouchTaskCallback(2, "espTouch fail ...");
            } else if (msg.what == 108) {
                String message = (String) msg.obj;
                listener.EspTouchTaskCallback(0, message);
            }else if (msg.what==109){
                listener.EspTouchTaskCallback(4, "can not recieve device message...");
            }
        }
    };


}
