package es.cursonoruego.lections;

import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import es.cursonoruego.R;
import es.cursonoruego.model.LectionEventJson;
import es.cursonoruego.model.LectionJson;
import es.cursonoruego.util.Log;

public class LectionEventListArrayAdapter extends ArrayAdapter<LectionEventJson> {

    private Context context;

    private List<LectionEventJson> lectionEvents;

    static class ViewHolder {
        TextView lectionTitle;
        ImageView reviewIcon1, reviewIcon2, reviewIcon3, reviewIcon4;
    }

    public LectionEventListArrayAdapter(Context context, List<LectionEventJson> lectionEvents) {
        super(context, android.R.layout.simple_list_item_1, lectionEvents);
        Log.d(getClass().getName(), "LectionEventListArrayAdapter");

        this.context = context;
        this.lectionEvents = lectionEvents;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(getClass().getName(), "getView");

        View listItem = convertView;
        if (listItem == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            listItem = layoutInflater.inflate(R.layout.activity_lection_reviews_list_item, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.lectionTitle = (TextView) listItem.findViewById(R.id.lection_events_list_item_lection_title);
            viewHolder.reviewIcon1 = (ImageView) listItem.findViewById(R.id.lection_events_list_item_review_icon_1);
            viewHolder.reviewIcon2 = (ImageView) listItem.findViewById(R.id.lection_events_list_item_review_icon_2);
            viewHolder.reviewIcon3 = (ImageView) listItem.findViewById(R.id.lection_events_list_item_review_icon_3);
            viewHolder.reviewIcon4 = (ImageView) listItem.findViewById(R.id.lection_events_list_item_review_icon_4);
            listItem.setTag(viewHolder);
        }

        LectionEventJson lectionEvent = lectionEvents.get(position);
       
        ViewHolder viewHolder = (ViewHolder) listItem.getTag();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
        String date = dateFormat.format(lectionEvent.getCalendar().getTime());
        Log.d(getClass().getName(), "date: " + date);
        viewHolder.lectionTitle.setText(lectionEvent.getLection().getTitle());
        viewHolder.lectionTitle.setTextColor(Color.parseColor("#000000"));

        if (lectionEvent.getCalendarRepetition1Completed() != null) {
            viewHolder.reviewIcon1.setImageResource(R.drawable.ic_done_green_600_24dp);

        } else if (lectionEvent.isTimeForRevision1()) {
            viewHolder.reviewIcon1.setImageResource(R.drawable.ic_autorenew_orange_600_24dp);

        } else {
            viewHolder.reviewIcon1.setImageResource(R.drawable.ic_https_grey_600_24dp);
        }

        if (lectionEvent.getCalendarRepetition2Completed() != null) {
            viewHolder.reviewIcon2.setImageResource(R.drawable.ic_done_green_600_24dp);

        } else if (lectionEvent.isTimeForRevision2()) {
            viewHolder.reviewIcon2.setImageResource(R.drawable.ic_autorenew_orange_600_24dp);

        } else {
            viewHolder.reviewIcon2.setImageResource(R.drawable.ic_https_grey_600_24dp);
        }

        if (lectionEvent.getCalendarRepetition3Completed() != null) {
            viewHolder.reviewIcon3.setImageResource(R.drawable.ic_done_green_600_24dp);

        } else if (lectionEvent.isTimeForRevision3()) {
            viewHolder.reviewIcon3.setImageResource(R.drawable.ic_autorenew_orange_600_24dp);

        } else {
            viewHolder.reviewIcon3.setImageResource(R.drawable.ic_https_grey_600_24dp);

        }

        if (lectionEvent.getCalendarRepetition4Completed() != null) {
            viewHolder.reviewIcon4.setImageResource(R.drawable.ic_done_green_600_24dp);

        } else if (lectionEvent.isTimeForRevision4()) {
            viewHolder.reviewIcon4.setImageResource(R.drawable.ic_autorenew_orange_600_24dp);

        } else {
            viewHolder.reviewIcon4.setImageResource(R.drawable.ic_https_grey_600_24dp);
        }
        
        return listItem;
    }
}
