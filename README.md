<p align="center">
  <img src="https://raw.githubusercontent.com/xuhongv/XSmartConfig-master/master/Asset/xSmartConfig.png" width="520px" height="300px" alt="Banner" />
</p>
 
 

----------

### 一. 前言。

 >之前逛gitHub，有人问过是否支持自定义回调8266的信息，我想应该可以把？所以，一直想动手搞上位机的配网封装，因为太忙了，这几天临近过年有点时间，终于搞好了。

>因为本人姓徐，所以命名为<font color =red> XSmartConfig , 顾名思义原理和smartConfig一样，只是再封装一次。</font>

----------

### 二. 特点。

- ①、继承了乐鑫8266的一键配网 smartConfig ， 保留了其功能。

- ②、支持多个设备配网的功。

- ③、支持隐藏的SSID配网。

- ④、目前V1.0版本仅仅支持配网成功后的UDP回调接收，具体的协议请自定义。

----------
### ③. 集成方法与使用。


----------
 - 为了兼顾 Eclipse 开发，同时缩短周期，我就不放在 jCenter 了，打包成jar架包大家使用吧。jar包下载地址在下面。


----------

- ①、所需的权限：
 ```
 <uses-permission android:name="android.permission.INTERNET" />
 <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
 <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
 
 ```
----------

 - ②、xEspTouchTask.java 类，是对外的API接口。<font color =red >**详细使用方法，请到Demo查看。**</font>

----------

```
 //这是我们平时用的smartConfig一键配网代码
 xEspTouchTask espTouchTask = new xEspTouchTask.Builder(this) //初始化
                    .setSsid(apSsid) //设置路由器的名字
                    .setPassWord(apPassword)  //设置路由器连接的密码
                    .creat(); //创建对象
            startSmartConfig();  //开始执行
```


----------
```
 //这是我们smartConfig一键配网后执行UDP接受过滤此设备的自定义一键配网
 xEspTouchTask espTouchTask = new xEspTouchTask.Builder(this) //初始化
                    .setSsid(apSsid) //设置路由器的名字
                    .setPassWord(apPassword)  //设置路由器连接的密码
                    .creat(); //创建对象
              espTouchTask.startSmartConfig(30, 8989);;  //开始执行带UDP协议的 一键配网，30是超时时间，8989是本地端口
```


----------
 - ③、设置监听接口 EspTouchTaskListener()。


----------
|code状态码|message信息回调|说明|
|--|--|
|0|表示成功配网，接着看message的信息，为一个包含设备的Mac地址和网关地址的Json|一键配网成功回调|
|1|为多个配网信息，还在配网中，其中message是刚刚配对成功的设备|只有设置了指定的个数配网设备才有回调|
|2|null|表示一键配网配网失败|
|3|表示成功接受到设备的UDP发来的自定义信息|仅当设置了端口号和超时时间才生效|
|4|null|表示超过了设置超时时间，未接受到设备的UDP信息|


----------


```   
        espTouchTask.setEspTouchTaskListener(new xEspTouchTask.EspTouchTaskListener() {
            @Override
            public void EspTouchTaskCallback(int code, String message) {
                switch (code) {
                    case 0:
                        //一键配网成功...
                        break;
                    case 1:
                        //为多个配网信息，还在配网中，其中message是刚刚配对成功的设备
                        break;   
                    case 2:    
                       //表示配网失败;        
                        break;    
                    case 3:                 
                        //配网成功后，UDP广播后获取到的信息：message
                        break;
                    case 4:
                       //表示超过了设置超时时间，未接受到设备的UDP信息
                        break;
                }
            }
        });
```


----------
### ④. 关于如何自定义设备成功连接路由器后的信息回调 。


----------

- 我说出来了这样的话语，那肯定有我解决的方法，鉴于我的想法如下，需要有点通讯知识才会看懂哦！
 
 - 设备成功连接路由器获取IP之后，确定您的设备和手机在同一个网段，之后设备开启UDP广播包，指定的远程端口号， 发送您的自定义信息。
 
 - 如果考虑到UDP广播不安全，您可以使用TCP协议，还记得8266那个smartConfig有个 手机的ip地址回调么？拿到这个地址之后，我们可以双方协议好，就好！

 - 但目前我仅仅做了UDP的。 如果您有想法，完全把我这份源码下载下来，做tcp协议。


----------

> 一句话概括：设备在配网成功获取IP之后，手机也会开启UDP接口来接受局域网的UDP信息包，这样就可以实行我们自定义的信息回调啦；<font color =red> 很多人会问，这样有什么好处，比如我设备已经连接路由器，但是不知道是否成功连接了MQTT服务器，您完全可以在连接MQTT之后再发送UD广播包，之后关闭。


----------

- Jar包下载：https://github.com/xuhongv/XSmartConfig-master/raw/master/Asset/XSmartConfig_V1.0.jar

- **APK下载体验**：

![等等](https://raw.githubusercontent.com/xuhongv/XSmartConfig-master/master/Asset/app_V1.png)

