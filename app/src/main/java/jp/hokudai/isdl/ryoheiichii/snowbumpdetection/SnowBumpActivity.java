package jp.hokudai.isdl.ryoheiichii.snowbumpdetection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by Ryohei Ichii on 2017/02/12.
 */

public class SnowBumpActivity extends FragmentActivity implements SensorEventListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, GoogleMap.OnMyLocationButtonClickListener, LocationSource{
    //加速度センサーとGoogleMapと位置情報のアブストラクトクラスをインプリメント

    private SensorManager mSensorManager; //加速度用センサーマネージャ
    private Sensor mAc; //加速度センサー
    private Sensor mMg; //方位センサー
    private float[] axis,mg = new float[3]; //センサーデータ
    private float deg; //方位角
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    //googlemap関連インスタンス

    private OnLocationChangedListener onLocationChangedListener = null;
    //ロケチェンリスナー

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); //スーパークラス
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //縦画面固定
        setContentView(R.layout.activity_snowbump); //画面描画

        Log.d("MainActivity","OnCreate Seq Start2."); //システムログ出力
        mSensorManager =(SensorManager)getSystemService(SENSOR_SERVICE); //センサーマネージャの取得
        locationRequest = locationRequest.create(); //ロケリスの作成（以下コンフィグ）
        //↓精度設定
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //高精度・高優先度に設定
        locationRequest.setInterval(1000); //更新頻度を1秒に設定
        locationRequest.setFastestInterval(16); //最高速更新頻度を16ミリ秒に設定

        mAc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //加速度センサーを登録
        mMg = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); //地磁気センサーを登録

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mymap);
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
        List<Sensor> sensors1 =
                mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER); //加速度センサーを取得
        if(sensors1.size()>0) { //センサーを取得できたら
            mSensorManager.registerListener(this,sensors1.get(0),SensorManager.SENSOR_DELAY_FASTEST);
            //端末の最高頻度で加速度を取得するリスナーを取得
        }

        List<Sensor> sensors2 =
                mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD); //地磁気センサーを取得
        if(sensors2.size()>0) { //センサーを取得できたら
            mSensorManager.registerListener(this,sensors2.get(0),SensorManager.SENSOR_DELAY_FASTEST);
            //端末の最高頻度で地磁気を取得するリスナーを取得
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
            CameraPosition.Builder cb = new CameraPosition.Builder(mMap.getCameraPosition());
            //カメラポジションビルダーの生成
            cb.bearing(deg); //方位に合わせカメラを回転
            cb.target(newLocation); //現在地をセット
            cb.zoom(17); //拡大倍率を17にセット
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cb.build())); //カメラを移動

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

        switch (event.sensor.getType()){ //どのセンサーの情報が更新されたかによって場合分け
            case Sensor.TYPE_ACCELEROMETER: //加速度センサーなら
                axis = event.values.clone(); //加速度データをクローン

                TextView textx = (TextView)findViewById(R.id.axisX);
                TextView texty = (TextView)findViewById(R.id.axisY);
                TextView textz = (TextView)findViewById(R.id.axisZ);
                //x,y,z軸のTextViewを取得
                textx.setText(Double.toString(axis[0]));
                texty.setText(Double.toString(axis[1]));
                textz.setText(Double.toString(axis[2]));
                //x,y,z軸の画面表示を更新
                break;
            case Sensor.TYPE_MAGNETIC_FIELD: //地磁気センサーなら
                mg = event.values.clone(); //地磁気データをクローン

                //↓地磁気から方位を求める
                float[] rotate = new float[16]; //傾斜行列
                float[] inclination = new float[16]; //回転行列

                if(axis!=null) { //加速度よりも地磁気が先に入力された場合のエラーを阻止するために場合分け（加速度がnullだと方位を割り出せない）
                    SensorManager.getRotationMatrix(rotate, inclination, axis, mg);
                    //マトリックス

                    // 方向を求める
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(rotate, orientation);

                    // デグリー角に変換する
                    deg = (float) Math.toDegrees(orientation[0]);
                }
                else{ //加速度データがnullなら
                    deg=0; //とりあえず真北を上に
                }
        }

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
