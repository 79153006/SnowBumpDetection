package jp.hokudai.isdl.ryoheiichii.snowbumpdetection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Ryohei Ichii on 2017/02/12.
 */

public class SnowBumpActivity extends FragmentActivity implements SensorEventListener{

    SensorManager mSensorManager;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); //スーパークラス
        setContentView(R.layout.activity_snowbump); //画面描画
        mSensorManager =(SensorManager)getSystemService(SENSOR_SERVICE); //センサーマネージャの取得

    }

    @Override
    protected void onResume(){ //センサー取得フェーズ
        super.onResume(); //スーパークラス
        List<Sensor> sensors =
                mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER); //加速度センサーを取得
        if(sensors.size()>0) { //センサーを取得できたら
            mSensorManager.registerListener(this,sensors.get(0),SensorManager.SENSOR_DELAY_FASTEST);
            //端末の最高頻度で加速度を取得するリスナーを取得
        }
    }

    @Override
    protected void onPause(){ //センサーが一時停止するとき
        super.onPause(); //スーパークラス
        mSensorManager.unregisterListener(this); //リスナー解除
    }

    @Override
    public void onSensorChanged(SensorEvent event){ //センサーの値が変更されたら

        double accuracy,ax,ay,az;

        accuracy =
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
    public void onAccuracyChanged(Sensor sensor,int accuracy){
        //センサーの精度が変更されたら
    }

}
