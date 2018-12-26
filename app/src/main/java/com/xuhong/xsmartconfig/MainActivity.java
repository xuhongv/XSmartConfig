package com.xuhong.xsmartconfig;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xuhong.xsmartconfiglib.api.xEspTouchTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private TextView mTvApSsid;
    private EditText mEdtApPassword;
    private Button btAdd;
    private CheckBox cbPaw;

    private WifiAdminUtils mWifiAdmin;
    private xEspTouchTask espTouchTask;


    //ui
    private ProgressDialog dialog;

    private static final int REQUEST_CODE = 205;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
    }


    private void checkPermission() {
        //是否大于6.0版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查是否已经授权
            int Code_ACCESS_FINE_LOCATION = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int Code_ACCESS_COARSE_LOCATION = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            //授权结果判断
            if (Code_ACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            } else if (Code_ACCESS_COARSE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
            } else {
                initView();
            }
        } else {
            initView();
        }
    }


    private void initView() {
        mWifiAdmin = new WifiAdminUtils(this);
        mTvApSsid = (TextView) findViewById(R.id.tvApSsid);
        mEdtApPassword = (EditText) findViewById(R.id.edApPassword);

        cbPaw = (CheckBox) findViewById(R.id.cbPaw);
        cbPaw.setVisibility(View.GONE);
        cbPaw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mEdtApPassword.setInputType(0x90);
                } else {
                    mEdtApPassword.setInputType(0x81);
                }
            }
        });

        btAdd = (Button) findViewById(R.id.btAdd);
        btAdd.setOnClickListener(this);

        mEdtApPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().isEmpty()) {
                    cbPaw.setVisibility(View.VISIBLE);
                } else {
                    cbPaw.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        // 展示当前显示已经连接的wifi
        String apSsid = mWifiAdmin.getWifiConnectedSsid();
        if (apSsid != null) {
            mTvApSsid.setText(apSsid);
            //保存在本地: ssid作为键  pas作为数值
            String password = SharedPreferencesUtils.getString(this, apSsid, "");
        } else {
            mTvApSsid.setText("");
        }
        //保存在本地: ssid作为键  pas作为数值
        String password = SharedPreferencesUtils.getString(this, apSsid, "");
        mEdtApPassword.setText(password);
        // check whether the wifi is connected
        boolean isApSsidEmpty = TextUtils.isEmpty(apSsid);
        btAdd.setEnabled(!isApSsidEmpty);


    }

    @Override
    public void onClick(View v) {
        if (v == btAdd) {
            String apSsid = mTvApSsid.getText().toString();
            String apPassword = mEdtApPassword.getText().toString();
            espTouchTask = new xEspTouchTask.Builder(this)
                    .setSsid(apSsid)
                    .setPassWord(apPassword)
                    .creat();
            startSmartConfig();

            //保存在本地: ssid作为键  pas作为数值
            SharedPreferencesUtils.putString(this, apSsid, apPassword);

        }

        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("努力配网中...");
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
                espTouchTask.stopSmartConfig();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
                finish();
            }
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (espTouchTask != null)
            espTouchTask.stopSmartConfig();
    }


    //配网后还带UDP的方法使用示范代码
    private void startConfigWithUDP() {
        espTouchTask.startSmartConfig(30, 8989);
        espTouchTask.setEspTouchTaskListener(new xEspTouchTask.EspTouchTaskListener() {
            @Override
            public void EspTouchTaskCallback(int code, String message) {
                Log.e("==w", "code:" + code + ",message:" + message);
                switch (code) {
                    case 0:
                        dialog.setMessage("配网成功，设备正在连接服务器...");
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
                        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        dialog.setMessage("设备连接服务器成功...");
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
                        Log.e("==w", "UDP广播后获取到的信息：" + message);
                        break;
                    case 2:
                    case 4:
                        dialog.setMessage("配网失败...");
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
                        break;
                }
            }
        });

    }


    //配网后不带UDP的使用示范
    private void startSmartConfig() {
        espTouchTask.startSmartConfig();
        espTouchTask.setEspTouchTaskListener(new xEspTouchTask.EspTouchTaskListener() {
            @Override
            public void EspTouchTaskCallback(int code, String message) {
                Log.e("==w", "code:" + code + ",message:" + message);
                switch (code) {
                    case 0:
                        dialog.setMessage("恭喜，配网成功!");
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
                        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        dialog.setMessage("配网失败...");
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
                        break;
                }
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0) {
                List<String> deniedPermission = new ArrayList<>();
                for (int i = 0; i < grantResults.length; i++) {
                    int grantResult = grantResults[i];
                    String permission = permissions[i];
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        deniedPermission.add(permission);
                    }
                }
                if (deniedPermission.isEmpty()) {
                    initView();
                } else {
                    Toast.makeText(this, "您拒绝了部分权限！可以在设置—应用详情授权，否则无法搜索wifi名字哦。", Toast.LENGTH_LONG).show();
                    initView();
                }

            }
        }
    }

}
