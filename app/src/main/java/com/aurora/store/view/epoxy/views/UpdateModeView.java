package com.aurora.store.view.epoxy.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.aurora.store.R;
import com.aurora.store.util.Common;

import java.util.List;
import java.util.Objects;

public class UpdateModeView {

    public static class AppData {
        public String label;
        public int updateMode;
    }

    public static class AppListAdapter extends ArrayAdapter<AppData> {

        private final LayoutInflater mInflater;

        public AppListAdapter(Context context, List<AppData> dataList) {
            super(context, R.layout.view_cpad_update_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(dataList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = new ViewHolder();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_cpad_update_item, parent, false);
                holder.textLabel = convertView.findViewById(R.id.label_cpad_update);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final AppData data = getItem(position);

            holder.textLabel.setText(data.label);

            /* RadioButtonの更新 */
            RadioButton button = convertView.findViewById(R.id.button_cpad_update);
            button.setChecked(isUpdater(data.updateMode));

            return convertView;
        }

        /* ランチャーに設定されているかの確認 */
        private boolean isUpdater(int i) {
            try {
                return Objects.equals(i, Common.GET_UPDATE_MODE(getContext()));
            } catch (NullPointerException ignored) {
                return false;
            }
        }
    }

    private static class ViewHolder {
        TextView textLabel;
    }
}