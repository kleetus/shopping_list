package com.kleetus.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ListActivity {
    public RequestQueue queue;
    private Gson gson;

    private FrameLayout mainFrame;
    private View progress;

    public List<Integer> checkedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);
        gson = new Gson();
        mainFrame = (FrameLayout) MainActivity.this.findViewById(R.id.container);
        progress = (MainActivity.this.getLayoutInflater()).inflate(R.layout.progress, null);
        checkedItems = new ArrayList<Integer>();
        getShoppingList();

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
        return super.onOptionsItemSelected(item);
    }

    private void clearCheckedList() {
        //send confirmation to clear and then clear
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
        if (checkedItems.size() < 1) {
            return;
        }
        showProgress();

        StringRequest request = new StringRequest(Request.Method.POST, MainApplication.server + "list/clear.json", new Response.Listener<String>() {
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
        StringRequest request = new StringRequest(Request.Method.POST, MainApplication.server
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
            }
        }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("item", item_name);
                params.put("quantity", quantity);
                return params;
            }
        };

        queue.add(request);
    }

    private void getShoppingList() {

        JsonArrayRequest request = new JsonArrayRequest(MainApplication.server + "list.json", new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray jsonArray) {
                setAdapter(jsonArray.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        }
        );
        queue.add(request);
    }

    private void setAdapter(String response) {
        Item[] items = gson.fromJson(response, Item[].class);
        ShoppingListAdapter adapter = new ShoppingListAdapter(this, R.layout.row, items);
        setListAdapter(adapter);
    }


    public static class Item {

        public String item;
        public int quantity;
        public int id;

        public String toString() {
            return item + " - " + quantity;
        }
    }

}
