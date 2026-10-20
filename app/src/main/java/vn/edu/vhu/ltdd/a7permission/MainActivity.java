package vn.edu.vhu.ltdd.a7permission;


import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends LabActivity {
    private static final String CHANNEL_ID="a7_channel";
    private static final int NOTI_ID=1001;
    private ImageView imgAnh;
    private TextView tvTrangThai,tvAnh;
    private final ActivityResultLauncher<String> xinQuyenCamera=registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),granted->{
            Log.d(TAG,"CAMERA result="+granted); capNhatTrangThai();
            if(granted) moCamera();
            else if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // Không tự hỏi dồn; lần bấm tiếp theo sẽ giải thích trước khi xin lại.
                Toast.makeText(this,R.string.camera_denied,Toast.LENGTH_SHORT).show();
            } else hienMoCaiDat();
        });
    private final ActivityResultLauncher<String> xinQuyenThongBao=registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),granted->{
            capNhatTrangThai();
            if(granted) guiThongBao(); else Toast.makeText(this,R.string.noti_denied,Toast.LENGTH_SHORT).show();
        });
    private final ActivityResultLauncher<Void> chupAnh=registerForActivityResult(
        new ActivityResultContracts.TakePicturePreview(),bitmap->{
            if(bitmap==null) { Toast.makeText(this,R.string.photo_cancelled,Toast.LENGTH_SHORT).show(); return; }
            showPhoto(bitmap);
            try(FileOutputStream out=new FileOutputStream(photoFile())) { bitmap.compress(Bitmap.CompressFormat.PNG,100,out); }
            catch(IOException e) { Log.w(TAG,"Không lưu được ảnh xem trước",e); }
        });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); showLayout(R.layout.activity_main);
        imgAnh=findViewById(R.id.imgAnh); tvTrangThai=findViewById(R.id.tvTrangThai); tvAnh=findViewById(R.id.tvAnh);
        taoKenhThongBao();
        if(photoFile().isFile()) { Bitmap b=BitmapFactory.decodeFile(photoFile().getPath()); if(b!=null) showPhoto(b); }
        findViewById(R.id.btnChupAnh).setOnClickListener(v->kiemTraRoiChup());
        findViewById(R.id.btnThongBao).setOnClickListener(v->kiemTraRoiGuiThongBao());
        findViewById(R.id.btnCaiDat).setOnClickListener(v->moCaiDatUngDung());
    }
    private File photoFile() { return new File(getFilesDir(),"preview.png"); }
    private void showPhoto(Bitmap b) {
        imgAnh.setImageBitmap(b); tvAnh.setText(getString(R.string.status_photo,b.getWidth(),b.getHeight()));
    }
    @Override protected void onResume() { super.onResume(); capNhatTrangThai(); }
    private void kiemTraRoiChup() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) moCamera();
        else if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) hienGiaiThich();
        else xinQuyenCamera.launch(Manifest.permission.CAMERA);
    }
    private void hienGiaiThich() {
        new AlertDialog.Builder(this).setTitle(R.string.rationale_title).setMessage(R.string.rationale_camera)
            .setPositiveButton(R.string.agree,(d,w)->xinQuyenCamera.launch(Manifest.permission.CAMERA))
            .setNegativeButton(R.string.cancel,null).show();
    }
    private void hienMoCaiDat() {
        new AlertDialog.Builder(this).setTitle(R.string.blocked_title).setMessage(R.string.blocked_camera)
            .setPositiveButton(R.string.open_settings,(d,w)->moCaiDatUngDung()).setNegativeButton(R.string.cancel,null).show();
    }
    private void moCamera() {
        try { chupAnh.launch(null); }
        catch(ActivityNotFoundException|SecurityException e) {
            Toast.makeText(this,R.string.camera_unavailable,Toast.LENGTH_SHORT).show(); Log.w(TAG,"Không mở được camera",e);
        }
    }
    private void moCaiDatUngDung() {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",getPackageName(),null))); }
        catch(ActivityNotFoundException e) { Toast.makeText(this,R.string.settings_unavailable,Toast.LENGTH_SHORT).show(); }
    }
    private void kiemTraRoiGuiThongBao() {
        if(Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) guiThongBao();
        else xinQuyenThongBao.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
    private void taoKenhThongBao() {
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel c=new NotificationChannel(CHANNEL_ID,getString(R.string.channel_name),NotificationManager.IMPORTANCE_DEFAULT);
            c.setDescription(getString(R.string.channel_desc)); getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    private void guiThongBao() {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return;
        Intent intent=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b=new NotificationCompat.Builder(this,CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(getString(R.string.noti_title))
            .setContentText(getString(R.string.noti_text)).setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true);
        try { NotificationManagerCompat.from(this).notify(NOTI_ID,b.build()); }
        catch(SecurityException e) { Log.w(TAG,"Quyền thông báo đã bị thu hồi",e); Toast.makeText(this,R.string.noti_denied,Toast.LENGTH_SHORT).show(); }
    }
    private void capNhatTrangThai() {
        if(tvTrangThai==null) return;
        boolean camera=ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED;
        boolean noti=NotificationManagerCompat.from(this).areNotificationsEnabled();
        if(Build.VERSION.SDK_INT>=33) noti=noti && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        tvTrangThai.setText(getString(R.string.status_format,camera?getString(R.string.granted):getString(R.string.denied),noti?getString(R.string.granted):getString(R.string.denied)));
        Log.d(TAG,tvTrangThai.getText().toString().replace('\n','|'));
    }
}
