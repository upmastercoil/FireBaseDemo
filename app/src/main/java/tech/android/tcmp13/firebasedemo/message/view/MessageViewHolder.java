package tech.android.tcmp13.firebasedemo.message.view;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import tech.android.tcmp13.firebasedemo.R;

/**
 * Created by noynngrisaru on 09/01/2017.
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {

	public TextView messageTextView;
	public TextView nameTextView;
	public ImageView photoImageView;

	public MessageViewHolder(View itemView) {

		super(itemView);
		messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
		nameTextView = (TextView) itemView.findViewById(R.id.nameTextView);
		photoImageView = (ImageView) itemView.findViewById(R.id.photoImageView);
	}
}
