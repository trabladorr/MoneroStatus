package tr.monerostatus.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import tr.monerostatus.DataContainer;
import tr.monerostatus.DataContainer.Refreshable;
import tr.monerostatus.R;
import tr.monerostatus.experimental.AlarmFragment;
import tr.monerostatus.experimental.GcmHandlerService;

public class MainActivity extends Activity implements Refreshable {
    public static final String PACKAGE_NAME = "tr.monerostatus";
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerArrayAdapter adapter = null;
    private Executor drawerExecutor = Executors.newSingleThreadExecutor();
    private static boolean fullscreen = false; //static to resist reset on rotation
    private static Set<Class<?>> baseRequirements = new HashSet<Class<?>>();

    private class DrawerArrayAdapter extends ArrayAdapter<String> {
        private List<String> titles;

        public DrawerArrayAdapter(Context context, int textViewResourceId, String[] titles) {

            super(context, textViewResourceId, titles);
            this.titles = new ArrayList<String>();
            this.titles.addAll(Arrays.asList(titles));
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflator.inflate(R.layout.drawer_entry, parent, false);

                ((TextView) convertView.findViewById(R.id.drawer_text_entry)).setText(titles.get(position));
            }

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerList = (ListView) findViewById(R.id.left_drawer_list);

        adapter = new DrawerArrayAdapter(this, R.layout.drawer_entry, getResources().getStringArray(R.array.fragments_pretty));
        mDrawerList.setAdapter(adapter);
        mDrawerList.setSelector(R.drawable.selection_selector);

        mDrawerList.setSelection(0);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.setSelected(true);

                String[] fragmentTags = getResources().getStringArray(R.array.fragment_tags);
                fragmentSelected(fragmentTags[position]);
            }
        });


        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);


        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        if (savedInstanceState == null)
            initFragment();

        //if (android.os.Build.VERSION.SDK_INT < 19)
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new OnSystemUiVisibilityChangeListener() {

                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if (visibility == 0)
                            new Handler().postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            setFullscreen(fullscreen);
                                        }
                                    }, 0);
                    }
                });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        NotificationManager mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(GcmHandlerService.NOTIFICATION_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataContainer.registerRefreshable(this);

        setFullscreen(fullscreen);

        if (!DataContainer.isFetching())
            DataContainer.fetchAllData(getResources());
        updateFragmentSettingsFromIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        DataContainer.setContinuous(false);
        DataContainer.unregisterRefreshable(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        DataContainer.setContinuous(false);
        DataContainer.unregisterRefreshable(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        setRefreshLongClickListener();
        return true;
    }

    //hack to set long click listener
    public void setRefreshLongClickListener() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final View v = findViewById(R.id.menu_refresh);

                if (v != null) {
                    v.setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View arg0) {
                            if (DataContainer.isContinuous())
                                DataContainer.setContinuous(false);
                            else {
                                DataContainer.setContinuous(true);
                                if (!DataContainer.isFetching())
                                    DataContainer.fetchRequiredData(getResources());
                            }
                            invalidateOptionsMenu();
                            return true;
                        }
                    });
                }
            }
        });
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (DataContainer.isContinuous()) {
            menu.findItem(R.id.menu_refresh).setIcon(ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_action_refresh_continuous));
            menu.findItem(R.id.menu_fullscreen).setTitle(getResources().getString(R.string.main_action_refresh_cont));
        } else if (DataContainer.isFetching()) {
            menu.findItem(R.id.menu_refresh).setIcon(ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_action_refresh_active));
            menu.findItem(R.id.menu_fullscreen).setTitle(getResources().getString(R.string.main_action_refresh_active));
        } else {
            menu.findItem(R.id.menu_refresh).setIcon(ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_action_refresh_inactive));
            menu.findItem(R.id.menu_fullscreen).setTitle(getResources().getString(R.string.main_action_refresh_inactive));
        }

        if (fullscreen) {
            menu.findItem(R.id.menu_fullscreen).setIcon(ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_action_return_from_full_screen));
            menu.findItem(R.id.menu_fullscreen).setTitle(getResources().getString(R.string.main_action_tofullscreen));
        } else {
            menu.findItem(R.id.menu_fullscreen).setIcon(ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_action_full_screen));
            menu.findItem(R.id.menu_fullscreen).setTitle(getResources().getString(R.string.main_action_fromfullscreen));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            if (DataContainer.isContinuous()) {
                DataContainer.setContinuous(false);
                return true;
            }
            DataContainer.fetchRequiredData(getResources());
            invalidateOptionsMenu();
            return true;
        } else if (id == R.id.menu_fullscreen) {
            fullscreen = !fullscreen;
            setFullscreen(fullscreen);
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void updateFragmentSettingsFromIntent(Intent intent) {

        Fragment frag = getFragmentManager().findFragmentById(R.id.fragment_container);
        Bundle extras = intent.getExtras();

        if (frag == null)
            return;
        if (frag.getClass().equals(InfoFragment.class) && extras != null) {
            ((InfoFragment) frag).setSpinnerCurrency(extras.getString("CURRENCY", null), null);
            ((InfoFragment) frag).setSpinnerExchange(extras.getString("EXCHANGE", null), null);
        } else if (frag.getClass().equals(CalcFragment.class) && extras != null) {
            ((CalcFragment) frag).setSpinnerCurrency(extras.getString("CURRENCY", null), null);
            ((CalcFragment) frag).setSpinnerExchange(extras.getString("EXCHANGE", null), null);
        }
    }

    public void refresh(Object lastData) {
        invalidateOptionsMenu();
    }

    @Override
    public Set<Class<?>> continuousRequirements() {
        return baseRequirements;
    }

    protected void initFragment() {
        Fragment frag = getFragmentManager().findFragmentById(R.id.fragment_container);
        if (frag != null)
            return;
        String fragmentTag = getIntent().getStringExtra("FRAGMENT");
        if (fragmentTag != null) {
            frag = createFragmentByTag(fragmentTag);
        } else {
            fragmentTag = InfoFragment.FRAGMENT_TAG;
            frag = new InfoFragment();
        }

        getFragmentManager().beginTransaction().add(R.id.fragment_container, frag, fragmentTag).commit();
    }

    public void fragmentSelected(String fragmentTag) {

        Fragment frag = getFragmentManager().findFragmentById(R.id.fragment_container);
        if (frag.getTag().equals(fragmentTag))
            return;

        frag = createFragmentByTag(fragmentTag);
        String fragmentTitle = Arrays.asList(getResources().getStringArray(R.array.fragments_pretty)).get(Arrays.asList(getResources().getStringArray(R.array.fragment_tags)).indexOf(fragmentTag));
        setTitle(fragmentTitle);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, frag, fragmentTag).commit();
    }

    //access the async sublist modifier
    public void setDrawerSublist(String fragmentTag, View sublist, boolean add) {
        new SublistModifier(fragmentTag, sublist, add).executeOnExecutor(drawerExecutor);
    }

    //modify fragment's entry in drawer to add or remove view
    private class SublistModifier extends AsyncTask<Void, Void, Void> {
        private String fragmentTag;
        private View sublist;
        private boolean add;
        private int retry = 3;


        public SublistModifier(String fragmentTag, View sublist, boolean add) {
            this.fragmentTag = fragmentTag;
            this.sublist = sublist;
            this.add = add;
        }

        private SublistModifier(String fragmentTag, View sublist, boolean add, int retry) {
            this.fragmentTag = fragmentTag;
            this.sublist = sublist;
            this.add = add;
            this.retry = retry;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            if (retry < 3)
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                }
            return null;
        }

        @Override
        protected void onPostExecute(Void arg0) {
            if (retry == 0)
                return;
            try {
                ListView drawer = (ListView) findViewById(R.id.left_drawer_list);
                ViewGroup layout = (ViewGroup) drawer.getChildAt(Arrays.asList(getResources().getStringArray(R.array.fragment_tags)).indexOf(fragmentTag));
                ViewGroup container = (ViewGroup) layout.findViewById(R.id.drawer_custom_container);
                if (add) {
                    if (container.getChildCount() > 0)
                        return;
                    container.addView(sublist);
                    Fragment frag = getFragmentManager().findFragmentByTag(fragmentTag);
                    if (fragmentTag.equals(GraphFragment.FRAGMENT_TAG)) {
                        ((GraphFragment) frag).updateGraph(true);
                    }
                } else
                    container.removeAllViews();

            } catch (Exception e) {
                new SublistModifier(fragmentTag, sublist, add, retry - 1).executeOnExecutor(drawerExecutor);
            }
        }

    }

    @SuppressLint("InlinedApi")
    private void setFullscreen(boolean set) {
        if (set) {
            if (android.os.Build.VERSION.SDK_INT < 19)
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            else
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else
            getWindow().getDecorView().setSystemUiVisibility(0);
    }

    public View getSublist(String fragmentTag) {
        try {
            ListView drawer = (ListView) findViewById(R.id.left_drawer_list);
            ViewGroup layout = (ViewGroup) drawer.getChildAt(Arrays.asList(getResources().getStringArray(R.array.fragment_tags)).indexOf(fragmentTag));
            ViewGroup container = (ViewGroup) layout.findViewById(R.id.drawer_custom_container);

            if (container.getChildCount() == 0)
                return null;
            return container.getChildAt(0);
        } catch (Exception e) {
        }

        return null;
    }

    /*
	private void updateFragmentSublist(){
		Fragment frag = getFragmentManager().findFragmentById(R.id.fragment_container);
		if (frag instanceof CustomSublistFragment)
			((CustomSublistFragment)frag).updateSublist(MainActivity.this);
	}
	*/
    public interface CustomSublistFragment {
        void updateSublist(MainActivity activity);
    }

    public static final List<Class<? extends Fragment>> fragments = new ArrayList<Class<? extends Fragment>>();

    static {
        fragments.add(InfoFragment.class);
        fragments.add(CalcFragment.class);
        fragments.add(GraphFragment.class);
        fragments.add(MarketFragment.class);
        fragments.add(AlarmFragment.class);
    }

    public Fragment createFragmentByTag(String fragmentTag) {
        for (Class<? extends Fragment> c : fragments) {
            String exchange;
            try {
                exchange = (String) c.getField("FRAGMENT_TAG").get(null);
                if (fragmentTag.equals(exchange)) {
                    return c.newInstance();
                }
            } catch (Exception e) {
            }
        }
        return null;
    }
}