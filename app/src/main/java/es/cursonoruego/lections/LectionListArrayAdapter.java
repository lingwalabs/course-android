package es.cursonoruego.lections;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import es.cursonoruego.model.LectionJson;
import es.cursonoruego.R;
import es.cursonoruego.util.Log;

public class LectionListArrayAdapter extends ArrayAdapter<LectionJson> {

    private Context context;

    private List<LectionJson> lections;

    static class ViewHolder {
        TextView lectionTitle;
        ImageView completedIcon;
    }

    public LectionListArrayAdapter(Context context, List<LectionJson> lections) {
        super(context, android.R.layout.simple_list_item_1, lections);
        Log.d(getClass().getName(), "LectionListArrayAdapter");

        this.context = context;
        this.lections = lections;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(getClass().getName(), "getView");

        View listItem = convertView;
        if (listItem == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            listItem = layoutInflater.inflate(R.layout.fragment_main_lections_list_item, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.lectionTitle = (TextView) listItem.findViewById(R.id.lections_list_item_lection_title);
            viewHolder.completedIcon = (ImageView) listItem.findViewById(R.id.lections_list_item_completed_icon);
            listItem.setTag(viewHolder);
        }

        LectionJson lection = lections.get(position);
       
        ViewHolder viewHolder = (ViewHolder) listItem.getTag();
        viewHolder.lectionTitle.setText(lection.getTitle());
        if (lection.isCompleted()) {
            viewHolder.completedIcon.setVisibility(View.VISIBLE);
        } else {
            viewHolder.completedIcon.setVisibility(View.GONE);
        }
        
        return listItem;
    }
}
