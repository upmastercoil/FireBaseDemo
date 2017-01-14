package tech.android.tcmp13.firebasedemo.message.control;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import tech.android.tcmp13.firebasedemo.R;
import tech.android.tcmp13.firebasedemo.message.model.Message;
import tech.android.tcmp13.firebasedemo.message.view.MessageViewHolder;

/**
 * Created by noynngrisaru on 09/01/2017.
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessageViewHolder> {

	private List<Message> messages;

	public MessagesAdapter() {

		this(new ArrayList<Message>());
	}

	public MessagesAdapter(List<Message> messages) {

		this.messages = messages;
	}

	@Override
	public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
		return new MessageViewHolder(view);
	}

	@Override
	public void onBindViewHolder(MessageViewHolder holder, int position) {

		Message message = messages.get(position);
		boolean hasPhoto = message.getPhotoUrl() != null;
		setupViewHolderContent(holder, message, hasPhoto);
		holder.nameTextView.setText(message.getName());
	}

	private void setupViewHolderContent(MessageViewHolder holder, Message message, boolean hasPhoto) {

		if (hasPhoto)
			showPhoto(holder, message);
		else
			showText(holder, message);

	}

	private void showPhoto(MessageViewHolder holder, Message message) {

		holder.messageTextView.setVisibility(View.GONE);
		holder.photoImageView.setVisibility(View.VISIBLE);
		Glide.with(holder.photoImageView.getContext()).load(message.getPhotoUrl()).into(holder.photoImageView);
	}

	private void showText(MessageViewHolder holder, Message message) {

		holder.photoImageView.setVisibility(View.GONE);
		holder.messageTextView.setVisibility(View.VISIBLE);
		holder.messageTextView.setText(message.getText());
	}

	@Override
	public int getItemCount() {

		return messages.size();
	}

	public void insert(Message message) {

		messages.add(0, message);
		notifyItemInserted(0);
	}

	public void clear() {

		messages.clear();
		notifyDataSetChanged();
	}
}
