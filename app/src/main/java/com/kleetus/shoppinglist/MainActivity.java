package com.kleetus.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;

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

public class MainActivity extends ListActivity {
    public RequestQueue queue;
    private Gson gson;
    private FrameLayout mainFrame;
    private View progress;
    public String username;
    public String password;
    public List<Integer> checkedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);
        gson = new Gson();
        mainFrame = (FrameLayout) MainActivity.this.findViewById(R.id.container);
        checkedItems = new ArrayList<Integer>();
        progress = (MainActivity.this.getLayoutInflater()).inflate(R.layout.progress, null);
        getCreds("username/pass");
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
        if (id == R.id.action_refresh) {
            refresh();
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
        new AlertDialog.Builder(this)
                .setMessage("Confirm that you want to clear the checked items?")
                .setCancelable(true)
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
        getCheckedItems();
        if (checkedItems.size() < 1) {
            return;
        }
        showProgress();

        StringRequest request = new BasicAuthStringRequest(this, Request.Method.POST, MainApplication.server + "list/clear.json", new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                checkedItems.clear();
                setAdapter(s);
                hideProgress();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                hideProgress();
                reauthIfNeeded(volleyError);
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
                add(((EditText) addDialog.findViewById(R.id.item_name)).getText().toString(), ((EditText) addDialog.findViewById(R.id.quantity)).getText().toString(), addDialog);
            }
        });
        addDialog.show();
    }

    private void add(final String item_name, final String quantity, final Dialog addDialog) {
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
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                hideProgress();
                reauthIfNeeded(volleyError);
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
        showProgress();

        StringRequest request = new BasicAuthStringRequest(this, Request.Method.GET, MainApplication.server + "list.json", new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                hideProgress();
                setAdapter(s);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                hideProgress();
                reauthIfNeeded(volleyError);
            }
        }
        );
        queue.add(request);
    }

    private void reauthIfNeeded(VolleyError error) {
        if (com.android.volley.AuthFailureError.class == error.getClass()) {
            getCreds("bad username/pass");
        }
    }

    private void setAdapter(String response) {
        Item[] items = gson.fromJson(response, Item[].class);
        View list = mainFrame.findViewById(android.R.id.list);
        if (items.length < 1) {
            list.setVisibility(View.GONE);
            LayoutInflater inflater = getLayoutInflater();
            View no_items = inflater.inflate(R.layout.empty_list, mainFrame, false);
            mainFrame.addView(no_items);
        } else {
            View no_items = mainFrame.findViewById(R.id.empty);
            if (null != no_items) {
                mainFrame.removeView(no_items);
            }
            list.setVisibility(View.VISIBLE);
            ShoppingListAdapter adapter = new ShoppingListAdapter(this, R.layout.row, items);
            setListAdapter(adapter);
        }

    }

    public void getCheckedItems() {
        ListView list = (ListView) mainFrame.findViewById(android.R.id.list);
        for (int i = 0; i < list.getCount(); i++) {
            View v = list.getChildAt(i);
            CheckBox box = (CheckBox) v.findViewById(R.id.checked);
            if (box.isChecked()) {
                checkedItems.add(Integer.valueOf(list.getItemAtPosition(i).toString()));
            }
        }
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
