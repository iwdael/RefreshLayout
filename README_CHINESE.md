# RefreshAndLoadLayout  [![](https://jitpack.io/v/aliletter/refreshandloadlayout.svg)](https://jitpack.io/#aliletter/refreshandloadlayout)
上拉刷新和下拉加载是列表中最常见的数据更新功能。RefreshAndLoadLayout不仅实现了这些功能，还为开发者提供了三个接口来实现相应的加载过渡效果。
## 使用说明
RefreshAndLoadLayout为使用者提供了三个接口，分别是onRefresh, onNormal, onLoose。对应的功能是开始刷新，停止刷新，松手刷新。在布局中，它必须有三个子控件，第一个为头部刷新控件，第二个为内容区域，第三个为尾部加载控件。
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

    <com.aliletter.refreshandloadlayout.RefreshAndLoadingLayout
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
    </com.aliletter.refreshandloadlayout.RefreshAndLoadingLayout>
</LinearLayout>
```
## 如何配置
将本仓库引入你的项目:
### Step 1. 添加JitPack仓库到Build文件
合并以下代码到项目根目录下的build.gradle文件的repositories尾。[点击查看详情](https://github.com/aliletter/CarouselBanner/blob/master/root_build.gradle.png)

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
### Step 2. 添加依赖
合并以下代码到需要使用的application Module的dependencies尾。[点击查看详情](https://github.com/aliletter/CarouselBanner/blob/master/application_build.gradle.png)
```Java
	dependencies {
	  ...
          compile 'com.github.aliletter:refreshandloadlayout:v1.2.2'
	}
```  
## Thank you for your browsing
如果你有任何疑问，请加入QQ群，我将竭诚为你解答。欢迎Star和Fork本仓库，当然也欢迎你关注我。
<br>
![Image Text](https://github.com/aliletter/CarouselBanner/blob/master/qq_group.png)
