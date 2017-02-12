package jp.hokudai.isdl.ryoheiichii.snowbumpdetection;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity { //MainActivityでは主にパーミッション関係の処理を行う


    private final int REQUEST_PERMISSION = 10; //パーミッションのリクエストコード(任意の番号(0以上が望ましい))

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); //スーパークラス
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //縦画面固定
        setContentView(R.layout.activity_main); //画面描画
        Log.d("MainActivity","OnCreate Seq Start."); //システムログ出力

        Button permitbutton = (Button)findViewById(R.id.permitsetbutton); //メイン画面でのパーミッション変更ボタン
        permitbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //変更ボタンが押されたら
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:"+ getPackageName() ));
                startActivity(intent); //Androidの設定画面へ遷移
            }
        });
        Button retrybutton = (Button)findViewById(R.id.retrybutton); //メイン画面での再試行ボタン
        retrybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //再試行ボタンが押されたら
                checkPermission(); //パーミッションテストを再実行
            }
        });


        if(Build.VERSION.SDK_INT >= 23){ //SDKのバージョンが23(Android6.0)以上であるなら
            checkPermission(); //チェックパーミッションメソッド実行（以下）
        }
        else{ //23より前のバージョンなら
            changeIntenttoApp(); //アプリ実行
            //6.0以上は個々のパーミッション指定が随時変更可能になったことに対応しての処理
        }

    }

    public void checkPermission(){ //チェックパーミッションメソッド
        if(ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //GPS取得(ACCESS_FINE_LOCATION)が許可されている場合
            changeIntenttoApp(); //アプリ実行
        }
        else{ //許可されていない場合
            requestLocationPermission(); //パーミッション要請メソッドを実行
        }
    }

    private void requestLocationPermission(){ //パーミッション要請メソッド
        if(!ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {
            //GPS取得が「永続的に」禁止されている場合（「今後も許可しない」を選択されている場合）
            Toast.makeText(this, R.string.jp_dialog_geopermissionwarn, Toast.LENGTH_LONG).show();
            //Toast表示で警告＆許可のお願いメッセージ
        }
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
        //GPS取得許可のパーミッション確認

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        //パーミッションリクエストの結果を返すメソッド
        if (requestCode == REQUEST_PERMISSION){ //リクエストコードとリザルトコードが一致した（許可された）場合
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                changeIntenttoApp(); //アプリ実行
                return;
            }
            else{ //一致しない（結局拒否された）場合
                AlertDialog.Builder ngalt = new AlertDialog.Builder(this); //警告ダイアログ
                ngalt.setTitle(R.string.jp_startfailed); //タイトル
                ngalt.setMessage(R.string.jp_dialog_geodeniedwarn); //メッセージ
                ngalt.setPositiveButton("OK", new DialogInterface.OnClickListener() { //ボタンと押されたときの処理
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //ボタン押下時の処理（↓はアプリ終了）
                        //finish();
                        //Process.killProcess(Process.myPid());
                    }
                });
                ngalt.show(); //アラートを表示
            }

        }
    }

    private void changeIntenttoApp(){ //アプリ実行メソッド
        Intent intent = new Intent(getApplication(),SnowBumpActivity.class); //インテント
        startActivity(intent); //切り替え
    }

}
