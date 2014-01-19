package com.kleetus.shoppinglist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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

        CheckBox box = (CheckBox) row.findViewById(R.id.checked);

        if (((MainActivity) context).checkedItems.contains(Integer.valueOf(objects[position].id))) {
            box.setChecked(true);
        }
        return row;
    }

}