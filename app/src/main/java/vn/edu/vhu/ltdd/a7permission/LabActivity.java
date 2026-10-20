package vn.edu.vhu.ltdd.a7permission;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class LabActivity extends AppCompatActivity {
    protected static final String TAG = "A7_231A290036";
    protected void showLayout(int layout) {
        EdgeToEdge.enable(this);
        setContentView(layout);
        View view = findViewById(R.id.main);
        int l=view.getPaddingLeft(), t=view.getPaddingTop();
        int r=view.getPaddingRight(), b=view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(l+bars.left,t+bars.top,r+bars.right,b+Math.max(bars.bottom,ime.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
    private void trace(String event) { Log.d(TAG,getClass().getSimpleName()+"."+event); }
    @Override protected void onCreate(Bundle state) { super.onCreate(state); trace("onCreate"); }
    @Override protected void onStart() { super.onStart(); trace("onStart"); }
    @Override protected void onResume() { super.onResume(); trace("onResume"); }
    @Override protected void onPause() { trace("onPause"); super.onPause(); }
    @Override protected void onStop() { trace("onStop"); super.onStop(); }
    @Override protected void onRestart() { super.onRestart(); trace("onRestart"); }
    @Override protected void onDestroy() { trace("onDestroy"); super.onDestroy(); }
}
