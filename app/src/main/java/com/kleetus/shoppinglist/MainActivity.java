package com.kleetus.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;


public class MainActivity extends ListActivity implements OnRefreshListener {
    public RequestQueue queue;
    private Gson gson;
    private FrameLayout mainFrame;
    private View progress;
    private String username;
    public List<Integer> checkedItems;
    private PullToRefreshLayout mPullToRefreshLayout;
    private ShoppingListAdapter adapter;
    private Item[] EMPTY = {};
    private boolean isRefreshing;
    private SharedPreferences preferences;
    private boolean isLoggedIn = false;
    private String session;

    private static final String SET_COOKIE_KEY = "Set-Cookie";
    private static final String COOKIE_KEY = "Cookie";
    private static final String SESSION_COOKIE = "rack.session";
    private static final String USERNAME = "username";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);
        gson = new Gson();
        mainFrame = (FrameLayout) MainActivity.this.findViewById(R.id.container);
        checkedItems = new ArrayList<Integer>();
        progress = (MainActivity.this.getLayoutInflater()).inflate(R.layout.progress, null);
        mPullToRefreshLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        username = getUsername();

        adapter = new ShoppingListAdapter(this, R.layout.row, EMPTY);
        setListAdapter(adapter);

        ActionBarPullToRefresh.from(this)
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);

        setSession();

        if (!isLoggedIn) {
            getCreds(getString(R.string.userpass));
        } else {
            getShoppingList();
        }

    }

    private void setSession() {
        String sessionId = preferences.getString(SESSION_COOKIE, "");
        if (sessionId.length() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append(SESSION_COOKIE);
            builder.append("=");
            builder.append(sessionId);
            session = builder.toString();
            isLoggedIn = true;
        }
    }

    private void getCreds(String message) {
        final Dialog credsDialog = new Dialog(this);
        credsDialog.setContentView(R.layout.auth);
        final EditText username_field = (EditText)credsDialog.findViewById(R.id.username);
        if(null != this.username) {
            username_field.setText(this.username);
        }
        credsDialog.setTitle(message);
        credsDialog.setCancelable(false);
        Button submit = ((Button) credsDialog.findViewById(R.id.auth_submit));
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = ((EditText) credsDialog.findViewById(R.id.password)).getText().toString();
                showProgress();
                credsDialog.hide();
                auth(username_field.getText().toString(), password, credsDialog);
            }
        });
        Button createAccount = (Button) credsDialog.findViewById(R.id.new_account);
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                credsDialog.hide();
                launchNewAccountDialog();
            }
        });
        credsDialog.show();
    }

    private void auth(final String u, final String p, final Dialog dialog) {
        StringRequest request = new ParamedStringRequest(this, Request.Method.POST, MainApplication.server + "session", new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                hideProgress();
                dialog.hide();
                Log.d("MainActivity", "Logged in successfully");
                isLoggedIn = true;
                getShoppingList();
                setUsername();
                MainActivity.this.username = u;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                hideProgress();
                dialog.show();
                dialog.setTitle("re-enter user/pass");
            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", u);
                params.put("password", p);
                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) { //incoming from server here, set cookie in user  here
                saveSessionCookie(response.headers);
                return super.parseNetworkResponse(response);
            }

        };
        queue.add(request);
    }

    private void setUsername() {
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.putString(USERNAME, username);
        prefEditor.commit();
    }


    private void launchNewAccountDialog() {
        final Dialog newAccountDialog = new Dialog(this);
        newAccountDialog.setContentView(R.layout.new_account);
        newAccountDialog.setTitle(R.string.new_account);
        newAccountDialog.setCancelable(false);
        Button back = (Button) newAccountDialog.findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newAccountDialog.hide();
                getCreds(getString(R.string.userpass));
            }
        });

        Button createAccount = (Button) newAccountDialog.findViewById(R.id.create_account);
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newAccount();
            }
        });

        newAccountDialog.show();
    }

    private void newAccount() {
        //check password and email address and send it in
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_login).setVisible(!isLoggedIn);
        menu.findItem(R.id.action_logout).setVisible(isLoggedIn);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            addItem();
        }
        if (id == R.id.action_clear_checked) {
            clearCheckedList();
        }
        if (id == R.id.action_check_all) {
            checkAll();
        }
        if (id == R.id.action_link_account) {
            linkAccount();
        }
        if (id == R.id.action_logout) {
            logout();
        }
        if (id == R.id.action_login) {
            getCreds(getString(R.string.userpass));
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        Toast.makeText(this, this.username + " " + getString(R.string.isLoggedOut), Toast.LENGTH_LONG).show();
        isLoggedIn = false;
        removeSession();
    }

    private void removeSession() {
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.remove(SESSION_COOKIE);
        prefEditor.commit();
    }

    private void linkAccount() {
        final Dialog linkAccountDialog = new Dialog(this);
        linkAccountDialog.setContentView(R.layout.link_account);
        linkAccountDialog.setCancelable(false);
        linkAccountDialog.setTitle(R.string.link_account);
        linkAccountDialog.show();
    }

    private void checkAll() {
        ListView list = (ListView) findViewById(android.R.id.list);
        for (int i = 0; i < list.getCount(); i++) {
            View layout = list.getChildAt(i);
            CheckBox box = (CheckBox) layout.findViewById(R.id.checked);
            box.setChecked(true);
        }
    }

    private void refresh() {
        getCheckedItems();
        getShoppingList();
    }

    private void clearCheckedList() {
        if(!isLoggedIn) {getCreds(getString(R.string.userpass));}
        getCheckedItems();
        if (checkedItems.size() < 1) {
            Toast.makeText(this, "No items were selected.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage("Confirm that you want to clear the checked items?")
                .setCancelable(false)
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clear();
                    }
                })
                .show();
    }

    private void clear() {
        if (isRefreshing) {
            return;
        }
        isRefreshing = true;

        showProgress();
        StringRequest request = new ParamedStringRequest(this, Request.Method.POST, MainApplication.server + "list/clear.json", new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                setAdapter(s);
                hideProgress();
                isRefreshing = false;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                hideProgress();
                setAdapter("[]");
                isRefreshing = false;
            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("items", gson.toJson(checkedItems));
                return params;
            }
        };
        queue.add(request);
    }

    private void showNetworkError() {
        Toast.makeText(this, R.string.network_problem, Toast.LENGTH_LONG).show();
    }

    private void hideProgress() {
        mainFrame.removeView(progress);
    }

    private void showProgress() {
        mainFrame.addView(progress);
    }

    private void addItem() {
        if(!isLoggedIn) {getCreds(getString(R.string.userpass));}
        final Dialog addDialog = new Dialog(this);
        addDialog.setContentView(R.layout.add_dialog);
        addDialog.setTitle("Add Item");
        Button go = ((Button) addDialog.findViewById(R.id.go));
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDialog.hide();
                add(((EditText) addDialog.findViewById(R.id.item_name)).getText().toString(), ((EditText) addDialog.findViewById(R.id.quantity)).getText().toString());
            }
        });
        addDialog.show();
    }

    private void add(final String item_name, final String quantity) {
        if (isRefreshing) {
            return;
        }
        isRefreshing = true;

        showProgress();
        getCheckedItems();
        int q = 1;
        try {
            q = Integer.parseInt(quantity);
        } catch (NumberFormatException e) {
        }
        final int i = q;

        StringRequest request = new ParamedStringRequest(this, Request.Method.POST, MainApplication.server
                + "/list/item.json", new Response.Listener<String>() {

            @Override
            public void onResponse(String s) {
                setAdapter(s);
                hideProgress();
                isRefreshing = false;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                hideProgress();
                setAdapter("[]");
                isRefreshing = false;
            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("item", item_name);
                params.put("quantity", String.valueOf(i));
                return params;
            }
        };

        queue.add(request);
    }

    private void getShoppingList() {
        if(!isLoggedIn) {getCreds(getString(R.string.userpass));}

        if (isRefreshing) {
            return;
        }
        isRefreshing = true;
        showProgress();

        StringRequest request = new ParamedStringRequest(this, Request.Method.GET, MainApplication.server + "list.json", new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                hideProgress();
                setAdapter(s);
                isRefreshing = false;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                hideProgress();
                setAdapter("[]");
                isRefreshing = false;
            }
        }
        );
        queue.add(request);
    }

    private void setAdapter(String response) {
        Item[] items = gson.fromJson(response, Item[].class);
        adapter.changeData(items);
    }

    public void getCheckedItems() {
        checkedItems.clear();
        ListView list = (ListView) mainFrame.findViewById(android.R.id.list);
        for (int i = 0; i < list.getCount(); i++) {
            View v = list.getChildAt(i);
            CheckBox box = (CheckBox) v.findViewById(R.id.checked);
            if (box.isChecked()) {
                checkedItems.add(Integer.valueOf(list.getItemAtPosition(i).toString()));
            }
        }
    }

    @Override
    public void onRefreshStarted(View view) {
        refresh();
        mPullToRefreshLayout.setRefreshComplete();
    }

    public String getUsername() {
        return preferences.getString(USERNAME, null);
    }

    public static class Item {

        public String item;
        public int quantity;
        public int id;

        public String toString() {
            return String.valueOf(id);
        }
    }

    private void saveSessionCookie(Map<String, String> headers) {
        if (headers.containsKey(SET_COOKIE_KEY)
                && headers.get(SET_COOKIE_KEY).startsWith(SESSION_COOKIE)) {
            String cookie = headers.get(SET_COOKIE_KEY);
            if (cookie.length() > 0) {
                String[] splitCookie = cookie.split(";");
                cookie = splitCookie[0];
                SharedPreferences.Editor prefEditor = preferences.edit();
                prefEditor.putString(SESSION_COOKIE, cookie);
                prefEditor.commit();
                this.session = cookie;
            }
        }
    }

    private void addSessionCookie(Map<String, String> headers) {
        if(isLoggedIn) {
            headers.put(COOKIE_KEY, session);
        }
    }


    public static class ParamedStringRequest extends StringRequest {
        Context context;

        public ParamedStringRequest(Context context, int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
            this.context = context;
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            Map<String, String> params = new HashMap<String, String>();
            return params;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError { //outgoing, set cookie TO server here
            Map<String, String> headers = super.getHeaders();
            if (headers == null || headers.equals(Collections.emptyMap())) {
                headers = new HashMap<String, String>();
            }
            ((MainActivity) context).addSessionCookie(headers);
            return headers;
        }
    }
}
