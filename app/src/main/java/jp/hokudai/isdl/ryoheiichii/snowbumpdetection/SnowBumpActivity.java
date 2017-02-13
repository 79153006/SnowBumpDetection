package jp.hokudai.isdl.ryoheiichii.snowbumpdetection;

import android.*;
import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.nio.BufferUnderflowException;
import java.util.List;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by Ryohei Ichii on 2017/02/12.
 */

public class SnowBumpActivity extends FragmentActivity implements SensorEventListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, GoogleMap.OnMyLocationButtonClickListener, LocationSource{
    //加速度センサーとGoogleMapと位置情報のアブストラクトクラスをインプリメント

    SensorManager mSensorManager; //加速度用センサーマネージャ
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    //googlemap関連インスタンス

    private OnLocationChangedListener onLocationChangedListener = null;
    //ロケチェンリスナー

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); //スーパークラス
        setContentView(R.layout.activity_snowbump); //画面描画

        Log.d("MainActivity","OnCreate Seq Start2."); //システムログ出力
        mSensorManager =(SensorManager)getSystemService(SENSOR_SERVICE); //センサーマネージャの取得
        locationRequest = locationRequest.create(); //ロケリスの作成（以下コンフィグ）
        //↓精度設定
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //高精度・高優先度に設定
        locationRequest.setInterval(1000); //更新頻度を1秒に設定
        locationRequest.setFastestInterval(16); //最高速更新頻度を16ミリ秒に設定

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.googlemap);

        //フラグメントとの紐付け
        mapFragment.getMapAsync(this); //Async

        //↓GoogleMapAPICliantの設定
        mGoogleApiClient = new GoogleApiClient
                .Builder(this).addApi(LocationServices.API) //API追加
                .addConnectionCallbacks(this) //コールバック
                .addOnConnectionFailedListener(this) //コネ失敗リスナー
                .build();
    }

    @Override
    protected void onResume(){ //Activityがアクティブ(?)になるときの処理
        super.onResume(); //スーパークラス
        List<Sensor> sensors =
                mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER); //加速度センサーを取得
        if(sensors.size()>0) { //センサーを取得できたら
            mSensorManager.registerListener(this,sensors.get(0),SensorManager.SENSOR_DELAY_FASTEST);
            //端末の最高頻度で加速度を取得するリスナーを取得
        }
        //OnResumeでMAPのAPIに接続
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause(){ //Activityがアクティブ(?)でなくなるときの処理
        super.onPause(); //スーパークラス
        mSensorManager.unregisterListener(this); //リスナー解除
        //OnPauseでAPIから切断
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onMapReady(GoogleMap googleMap){ //Mapがアクティブになるときの処理
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //パーミッションチェック（MainActivityでチェックしているが、起動中にパーミッションが変更される可能性があるため）
            mMap = googleMap; //地図のインスタンスにGoogleMapを登録
            mMap.setLocationSource(this); //位置情報のソースを自分に切り替え
            mMap.setMyLocationEnabled(true); //自分の位置情報の利用を許可
        }
        else{ //パーミッションがなかったら
            Toast.makeText(this,R.string.jp_permissionchk,Toast.LENGTH_LONG).show(); //Toastによる警告
            Intent intent = new Intent(getApplication(),MainActivity.class); //MainActivityへのインテント
            startActivity(intent); //切り替え
        } //再度パーミッション変更要請をする
    }

    @Override
    public void onLocationChanged(Location location){ //位置データが変更されたら
        if(onLocationChangedListener != null){ //位置情報変更のリスナーがあれば
            onLocationChangedListener.onLocationChanged(location); //位置情報の変更をリスナーに登録

            double lat = location.getLatitude(); //緯度を受け取り
            double lng = location.getLongitude(); //経度を受け取り

            //↓マップの更新
            LatLng newLocation = new LatLng(lat,lng); //LatLngクラスに位置情報を登録
            mMap.moveCamera((CameraUpdateFactory.newLatLng(newLocation))); //カメラの中心を現在位置に移動
        }
    }

    @Override
    public void onConnected(Bundle bundle){ //GoogleAPI用接続時の処理
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //パーミッションチェック（MainActivityでチェックしているが、起動中にパーミッションが変更される可能性があるため）
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,locationRequest, this);
            //FusedLocationAPIの利用（GPS,3G/4G,Wi-Fiの位置情報を混合的に利用できる
        }
        else{ //パーミッションがなかったら
            Toast.makeText(this,R.string.jp_permissionchk,Toast.LENGTH_LONG).show(); //Toastによる警告
            Intent intent = new Intent(getApplication(),MainActivity.class); //MainActivityへのインテント
            startActivity(intent); //切り替え
        } //再度パーミッション変更要請をする
    }

    @Override
    public void onConnectionSuspended(int i){ //GoogleAPI接続が休止したときの処理

    }

    @Override
    public void  onConnectionFailed(ConnectionResult connectionResult){ //GoogleAPIの接続に失敗したときの処理

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener){ //ロケチェンリスナーが呼ぶ処理
        this.onLocationChangedListener =onLocationChangedListener; //リスナー登録
    }

    @Override
    public void deactivate(){
        this.onLocationChangedListener = null; //リスナー解除
    }

    @Override
    public void onSensorChanged(SensorEvent event){ //加速度センサーの値が変更されたときに読み込まれる

        double ax,ay,az;

        ax = event.values[0]; //x軸取得
        ay = event.values[1]; //y軸取得
        az = event.values[2]; //z軸取得

        TextView textx = (TextView)findViewById(R.id.axisX);
        TextView texty = (TextView)findViewById(R.id.axisY);
        TextView textz = (TextView)findViewById(R.id.axisZ);
        //x,y,z軸のTextViewを取得
        textx.setText(Double.toString(ax));
        texty.setText(Double.toString(ay));
        textz.setText(Double.toString(az));
        //x,y,z軸の画面表示を更新
    }

    @Override
    public boolean onMyLocationButtonClick(){ //現在位置ボタンがタップされたときの処理
        return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor,int accuracy){
        //加速度センサーの精度が変更されたときの処理
    }

}
