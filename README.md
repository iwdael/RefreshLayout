# RefreshLayout  [![](https://jitpack.io/v/blackchopper/refreshlayout.svg)](https://jitpack.io/#blackchopper/refreshlayout)
Pull-up and pull-down loads are the most common data update features in the list. RefreshAndLoadLayout not only implements these functions, but also provides developers with three interfaces to achieve the appropriate load transition.[中文文档](https://github.com/blackchopper/RefreshAndLoadLayout/blob/master/README_CHINESE.md)
## Instruction
RefreshAndLoadLayout provides three interfaces to the user: onRefresh, onNormal, onLoose. Corresponding function is to start refreshing, stop refreshing, let go refresh. In the layout, it must have three child controls, the first for the head refresh control, the second for the content area, and the third for the tail load control.
### Sample Code
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
```Java
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.blackchopper.refreshandloadlayout.RefreshAndLoadingLayout
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
    </com.blackchopper.refreshandloadlayout.RefreshAndLoadingLayout>
</LinearLayout>
```
## How to
To get a Git project into your build:
### Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories.[click here for details](https://github.com/blackchopper/CarouselBanner/blob/master/root_build.gradle.png)

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
### Step 2. Add the dependency
Add it in your application module build.gradle at the end of dependencies where you want to use.   [click here for details](https://github.com/blackchopper/CarouselBanner/blob/master/application_build.gradle.png)
```Java
	dependencies {
	  ...
          compile 'com.github.blackchopper:refreshlayout:v1.2.3'
	}
```	
## Thank you for your browsing
If you have any questions, please join the QQ group. I will do my best to answer it for you. Welcome to star and fork this repository, alse follow me.
<br>
![Image Text](https://github.com/blackchopper/CarouselBanner/blob/master/qq_group.png)
