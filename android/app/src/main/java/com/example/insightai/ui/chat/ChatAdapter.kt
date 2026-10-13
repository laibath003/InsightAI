
package com.example.insightai.ui.chat

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.insightai.R
import com.example.insightai.data.model.ChatMessage
import io.noties.markwon.Markwon

class ChatAdapter(context: Context) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private var messages = emptyList<ChatMessage>()
    private val markwon = Markwon.create(context)

    fun submitList(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) 0 else 1
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolder {

        val layout = if (viewType == 0) {
            R.layout.item_user_message
        } else {
            R.layout.item_ai_message
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(layout, parent, false)

        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MessageViewHolder,
        position: Int
    ) {
        holder.bind(messages[position], markwon)
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val textView: TextView =
            itemView.findViewById(R.id.textViewMessage)

        private val sourcesView: TextView? =
            itemView.findViewById(R.id.textViewSources)

        private val researchModeView: TextView? =
            itemView.findViewById(R.id.textViewResearchMode)

        fun bind(
            message: ChatMessage,
            markwon: Markwon
        ) {
            if (message.isUser) {
                textView.text = message.message
                return
            }

            // Display the research mode selected for this answer.
            researchModeView?.let { view ->
                val label = when (message.researchMode?.uppercase()) {
                    "WEB" -> "WEB RESEARCH"
                    "DOCUMENT" -> "DOCUMENT RESEARCH"
                    "BOTH" -> "WEB + DOCUMENTS"
                    else -> null
                }

                if (label == null) {
                    view.text = ""
                    view.visibility = View.GONE
                } else {
                    view.text = label
                    view.visibility = View.VISIBLE
                }
            }

            // Render the AI response as Markdown.
            markwon.setMarkdown(textView, message.message)

            // Preserve clickable web source links.
            sourcesView?.let { view ->
                if (message.sources.isEmpty()) {
                    view.text = ""
                    view.visibility = View.GONE
                } else {
                    val sourcesMarkdown = buildString {
                        append("### Web Sources\n\n")

                        message.sources.forEachIndexed { index, source ->
                            val safeTitle = source.title
                                .replace("[", "\\[")
                                .replace("]", "\\]")

                            append(
                                "${index + 1}. [$safeTitle](${source.url})\n"
                            )
                        }
                    }

                    view.visibility = View.VISIBLE
                    markwon.setMarkdown(view, sourcesMarkdown)
                    view.movementMethod =
                        LinkMovementMethod.getInstance()
                }
            }
        }
    }
}