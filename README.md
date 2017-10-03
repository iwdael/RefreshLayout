# RefreshAndLoadingLayout  [![](https://www.jitpack.io/v/mr-absurd/RefreshAndLoadLayout.svg)](https://www.jitpack.io/#mr-absurd/RefreshAndLoadLayout)
Pull-up refresh and drop-down is the most commonly used in the development of Andrews data refresh and load function. RefreshAndLoadLayout implements the above functions, which provides three interfaces for developers to implement the corresponding animation (onRefresh, onNormal, onLoose), it also provides a pull-up refresh and pull-down load switch settings, to meet most of the development.
# How to
To get a Git project into your build:
## Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
## Step 2. Add the dependency

	dependencies {
	        compile 'com.github.mr-absurd:refreshandloadlayout:v1.0.0'
	}
  
  
# Instructions
## Java Code
```java
public class MainActivity extends Activity implements RefreshAndLoadingLayout.OnRefreshListener {
    private RefreshAndLoadingLayout mSwipeLayout;
    private WebView mPage;
    private TextView mHint, mHinp;
    ...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
	...
        mSwipeLayout = (RefreshAndLoadingLayout) findViewById(R.id.swipe_container);
        mPage = (WebView) findViewById(R.id.page);
        mHint = (TextView) findViewById(R.id.hint);
        mHinp = (TextView) findViewById(R.id.hinp);
        mSwipeLayout.setOnRefreshListener(this);
	...
    }

    //mCurrent represent refresh or load
    @Override
    public void onRefresh(boolean mCurrent) {
        mHint.setText("正在刷新，请等待");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 停止刷新
                mSwipeLayout.setRefreshing(false);
                mSwipeLayout.stopRefresh();
		...
            }
        }, 3000);
    }

    //mCurrent represent refresh or load
    @Override
    public void onNormal(boolean mCurrent) {
        if (mCurrent)
            mHint.setText("下拉刷新");
        else
            mHinp.setText("下拉刷新");

    }

    //mCurrent represent refresh or load
    @Override
    public void onLoose(boolean mCurrent) {
        if (mCurrent)
            mHint.setText("松手刷新");
        else
            mHinp.setText("松手刷新");
    }
}
```
## XML Code 
RefreshAndLoadingLayout must hava three child only.
```Java
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.absurd.library.RefreshAndLoadingLayout
        android:id="@+id/swipe_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
	app:loadEnabled="false"
        app:refreshEnabled="false"
        android:scrollbars="none">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="10dp">
            <TextView
                android:id="@+id/hint"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:text="下拉刷新"
                android:textColor="#000"
                android:textSize="20sp" />
        </LinearLayout>



        <WebView
            android:id="@+id/page"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
	    
	    
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="10dp">
            <TextView
                android:id="@+id/hinp"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:text="下拉刷新"
                android:textColor="#000"
                android:textSize="20sp" />

        </LinearLayout>
    </com.absurd.library.RefreshAndLoadingLayout>


</LinearLayout>

```
