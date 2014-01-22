package com.kleetus.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Base64;
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
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.util.ArrayList;
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
    public String username;
    public String password;
    public List<Integer> checkedItems;
    private PullToRefreshLayout mPullToRefreshLayout;
    private ShoppingListAdapter adapter;
    private Item[] EMPTY = {};
    private boolean isRefreshing;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);
        gson = new Gson();
        mainFrame = (FrameLayout) MainActivity.this.findViewById(R.id.container);
        checkedItems = new ArrayList<Integer>();
        progress = (MainActivity.this.getLayoutInflater()).inflate(R.layout.progress, null);
        getCreds(getString(R.string.userpass));
        mPullToRefreshLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);
        adapter = new ShoppingListAdapter(this, R.layout.row, EMPTY);
        setListAdapter(adapter);

        ActionBarPullToRefresh.from(this)
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
    }

    private void getCreds(String message) {
        final Dialog credsDialog = new Dialog(this);
        credsDialog.setContentView(R.layout.auth);
        credsDialog.setTitle(message);
        Button submit = ((Button) credsDialog.findViewById(R.id.auth_submit));
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                credsDialog.hide();
                username = ((EditText) credsDialog.findViewById(R.id.username)).getText().toString();
                password = ((EditText) credsDialog.findViewById(R.id.password)).getText().toString();
                getShoppingList();
            }
        });
        credsDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
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
        return super.onOptionsItemSelected(item);
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
        StringRequest request = new BasicAuthStringRequest(this, Request.Method.POST, MainApplication.server + "list/clear.json", new Response.Listener<String>() {
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
                if (!reauthIfNeeded(volleyError)) {
                    showNetworkError();
                }
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

        StringRequest request = new BasicAuthStringRequest(this, Request.Method.POST, MainApplication.server
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
                if (!reauthIfNeeded(volleyError)) {
                    showNetworkError();
                }
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
        if (isRefreshing) {
            return;
        }
        isRefreshing = true;
        showProgress();

        StringRequest request = new BasicAuthStringRequest(this, Request.Method.GET, MainApplication.server + "list.json", new Response.Listener<String>() {
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
                if (!reauthIfNeeded(volleyError)) {
                    showNetworkError();
                }
                isRefreshing = false;
            }
        }
        );
        queue.add(request);
    }

    private boolean reauthIfNeeded(VolleyError error) {
        if (com.android.volley.AuthFailureError.class == error.getClass()) {
            getCreds("bad username/pass");
            return true;
        }
        return false;
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

    public static class Item {

        public String item;
        public int quantity;
        public int id;

        public String toString() {
            return String.valueOf(id);
        }
    }


    public static class BasicAuthStringRequest extends StringRequest {
        Context context;

        public BasicAuthStringRequest(Context context, int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
            this.context = context;
        }


        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            Map<String, String> params = new HashMap<String, String>();
            params.put("grant_type", "client_credentials");
            return params;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = new HashMap<String, String>();
            String auth = "Basic "
                    + Base64.encodeToString((((MainActivity) context).username + ":" + ((MainActivity) context).password).getBytes(), Base64.NO_WRAP);
            headers.put("Authorization", auth);
            return headers;
        }
    }
}
