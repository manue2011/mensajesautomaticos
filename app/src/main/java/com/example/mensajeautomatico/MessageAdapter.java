package com.example.mensajeautomatico;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adaptador para el RecyclerView que muestra los mensajes en DashboardActivity.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final Context context;
    private final List<Message> messageList;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.tvPhoneNumber.setText(message.getPhoneNumber());
        holder.tvMessageText.setText(message.getMessageText());
        holder.tvTimestamp.setText(message.getTimestamp());
        holder.tvStatus.setText(message.getStatus());

        // Cambiar color según el estado
        switch (message.getStatus()) {
            case "Enviado":
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.green));
                break;
            case "Pendiente":
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.orange));
                break;
            case "Error":
                holder.tvStatus.setTextColor(context.getResources().getColor(R.color.red));
                break;
            default:
                holder.tvStatus.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvPhoneNumber, tvMessageText, tvTimestamp, tvStatus;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhoneNumber = itemView.findViewById(R.id.tv_phone_number);
            tvMessageText = itemView.findViewById(R.id.tv_message_text);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}