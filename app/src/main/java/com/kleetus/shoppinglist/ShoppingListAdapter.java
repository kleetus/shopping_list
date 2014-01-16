package com.kleetus.shoppinglist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import junit.framework.Assert;


public class ShoppingListAdapter extends ArrayAdapter<MainActivity.Item> {

    Context context;
    MainActivity.Item[] objects;

    public ShoppingListAdapter(Context context, int resource, MainActivity.Item[] objects) {
        super(context, resource, objects);
        this.context = context;
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View row = inflater.inflate(R.layout.row, parent, false);
        TextView itemLabel = (TextView) row.findViewById(R.id.item_label);
        TextView quanityLabel = (TextView) row.findViewById(R.id.quantity_label);

        Assert.assertNotNull("itemLabel should never be null here.", itemLabel);
        Assert.assertNotNull("quantityLabel should never be null here.", quanityLabel);

        itemLabel.setText(objects[position].item);

        quanityLabel.setText(String.valueOf(objects[position].quantity));

        handleCheckedClick(row, objects[position].id);

        return row;
    }

    private void handleCheckedClick(View row, final Integer id) {
        row.findViewById(R.id.checked).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((MainActivity) context).checkedItems.contains(id)) {
                    ((MainActivity) context).checkedItems.remove(id);
                } else {
                    ((MainActivity) context).checkedItems.add(id);
                }

            }
        });
    }
}
