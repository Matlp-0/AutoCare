package com.example.myapplication.ui.schedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.model.RevisionMilestone;
import com.example.myapplication.ui.carbon.CarbonSectionTitle;
import com.example.myapplication.util.Formatters;

import java.util.ArrayList;
import java.util.List;

/**
 * Timeline técnica das revisões: trilho contínuo por quilometragem, com um
 * cabeçalho separando o que já passou do que está planejado.
 *
 * <p>Só apresentação: os marcos e estados continuam vindo do domínio, e marcar
 * como realizada usa o mesmo callback de antes.
 */
public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnRevisionChecked {
        void onChecked(RevisionMilestone milestone, boolean done);
    }

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_MILESTONE = 1;

    private static class Row {
        int type;
        int sectionRes;
        RevisionMilestone milestone;
        boolean lastOfList;
    }

    private final List<Row> rows = new ArrayList<>();
    private final OnRevisionChecked listener;

    public TimelineAdapter(OnRevisionChecked listener) {
        this.listener = listener;
    }

    public void submit(List<RevisionMilestone> milestones, int currentKm) {
        rows.clear();
        if (milestones != null) {
            boolean historyHeaderAdded = false;
            boolean plannedHeaderAdded = false;
            for (RevisionMilestone milestone : milestones) {
                boolean planned = milestone.km > currentKm;
                if (!planned && !historyHeaderAdded) {
                    rows.add(section(R.string.carbon_section_history));
                    historyHeaderAdded = true;
                } else if (planned && !plannedHeaderAdded) {
                    rows.add(section(R.string.carbon_section_planned));
                    plannedHeaderAdded = true;
                }
                Row row = new Row();
                row.type = TYPE_MILESTONE;
                row.milestone = milestone;
                rows.add(row);
            }
        }
        if (!rows.isEmpty()) {
            rows.get(rows.size() - 1).lastOfList = true;
        }
        notifyDataSetChanged();
    }

    private Row section(int titleRes) {
        Row row = new Row();
        row.type = TYPE_SECTION;
        row.sectionRes = titleRes;
        return row;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SECTION) {
            return new SectionViewHolder(
                    inflater.inflate(R.layout.item_carbon_section, parent, false));
        }
        return new MilestoneViewHolder(
                inflater.inflate(R.layout.item_milestone, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof SectionViewHolder) {
            ((SectionViewHolder) holder).bind(row.sectionRes);
        } else {
            ((MilestoneViewHolder) holder).bind(row.milestone, row.lastOfList, listener);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {

        private final CarbonSectionTitle title;

        SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.sectionTitle);
        }

        void bind(int titleRes) {
            title.setText(itemView.getContext().getString(titleRes));
        }
    }

    static class MilestoneViewHolder extends RecyclerView.ViewHolder {

        private final TextView textKm;
        private final TextView textStateLabel;
        private final TextView textItems;
        private final TextView checkDone;
        private final View marker;
        private final View railTop;
        private final View railBottom;

        MilestoneViewHolder(@NonNull View itemView) {
            super(itemView);
            textKm = itemView.findViewById(R.id.textKm);
            textStateLabel = itemView.findViewById(R.id.textStateLabel);
            textItems = itemView.findViewById(R.id.textItems);
            checkDone = itemView.findViewById(R.id.checkDone);
            marker = itemView.findViewById(R.id.marker);
            railTop = itemView.findViewById(R.id.railTop);
            railBottom = itemView.findViewById(R.id.railBottom);
        }

        void bind(final RevisionMilestone milestone, boolean lastOfList,
                  final OnRevisionChecked listener) {
            textKm.setText(Formatters.km(milestone.km));
            textItems.setText(milestone.itemsText());

            int stateColor = ContextCompat.getColor(itemView.getContext(),
                    colorRes(milestone.state));
            textStateLabel.setText(stateLabelRes(milestone.state));
            textStateLabel.setTextColor(stateColor);
            marker.setBackgroundResource(markerRes(milestone.state));

            railTop.setVisibility(getBindingAdapterPosition() == 0 ? View.INVISIBLE : View.VISIBLE);
            railBottom.setVisibility(lastOfList ? View.INVISIBLE : View.VISIBLE);

            boolean done = milestone.state == RevisionMilestone.State.DONE;
            checkDone.setSelected(done);
            checkDone.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.carbon_check_box, 0, 0, 0);
            checkDone.setText(done && !milestone.userMarked
                    ? R.string.schedule_from_history
                    : R.string.schedule_mark_done);
            checkDone.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    done ? R.color.carbon_accent : R.color.carbon_text_secondary));
            checkDone.setContentDescription(itemView.getContext().getString(
                    done ? R.string.schedule_marked_by_user : R.string.schedule_mark_done));
            checkDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onChecked(milestone,
                                milestone.state != RevisionMilestone.State.DONE);
                    }
                }
            });
        }

        /** Texto do estado fica em recursos: o domínio não carrega apresentação. */
        private int stateLabelRes(RevisionMilestone.State state) {
            switch (state) {
                case DONE:
                    return R.string.carbon_revision_done;
                case NEXT:
                    return R.string.carbon_revision_next;
                case LATE:
                    return R.string.carbon_revision_late;
                case NO_RECORD:
                    return R.string.carbon_revision_no_record;
                default:
                    return R.string.carbon_revision_future;
            }
        }

        private int colorRes(RevisionMilestone.State state) {
            switch (state) {
                case DONE:
                    return R.color.carbon_cyan;
                case LATE:
                    return R.color.carbon_alert;
                case NEXT:
                    return R.color.carbon_accent;
                default:
                    return R.color.carbon_text_dim;
            }
        }

        private int markerRes(RevisionMilestone.State state) {
            switch (state) {
                case DONE:
                    return R.drawable.carbon_marker_done;
                case LATE:
                    return R.drawable.carbon_marker_late;
                case NEXT:
                    return R.drawable.carbon_marker_next;
                case NO_RECORD:
                    return R.drawable.carbon_marker_norecord;
                default:
                    return R.drawable.carbon_marker_future;
            }
        }
    }
}
