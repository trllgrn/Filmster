package filmster.com.example.com.filmster;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String poster_url = null;
        float default_rating = 0;
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        ImageView posterView = (ImageView) rootView.findViewById(R.id.film_detail_poster);
        TextView titleTextView = (TextView) rootView.findViewById(R.id.film_title);
        TextView synopsisTextView = (TextView) rootView.findViewById(R.id.film_synopsis);
        TextView releaseView = (TextView) rootView.findViewById(R.id.film_release);
        TextView ratingView = (TextView) rootView.findViewById(R.id.film_rating);

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            if (intent.hasExtra(getString(R.string.detail_title))) {
                titleTextView.setText(intent.getStringExtra(getString(R.string.detail_title)));
            }

            if (intent.hasExtra(getString(R.string.detail_synopsis))) {
                synopsisTextView.setText(intent.getStringExtra(getString(R.string.detail_synopsis)));
            }

            if (intent.hasExtra(getString(R.string.detail_release))) {
                releaseView.setText(intent.getStringExtra(getString(R.string.detail_release)));
            }

            if (intent.hasExtra(getString(R.string.detail_rating))) {
                ratingView.setText(new Float(intent.getFloatExtra(getString(R.string.detail_rating),default_rating)).toString());
            }

            if(intent.hasExtra(getString(R.string.detail_poster))) {
                poster_url = intent.getStringExtra(getString(R.string.detail_poster));
            }
        }

        Picasso.with(rootView.getContext()).load(poster_url).into(posterView);

        return rootView;
    }
}
