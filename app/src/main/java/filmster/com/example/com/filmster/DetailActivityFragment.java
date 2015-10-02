package filmster.com.example.com.filmster;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {
    //Layout variable resources
    private LinearLayout topView = null;
    private LinearLayout reviewSectionTop = null;
    private LinearLayout trailersHolderView = null;
    private ImageView posterView = null;
    private TextView titleTextView = null;
    private TextView synopsisTextView = null;
    private TextView releaseView = null;
    private TextView ratingView = null;
    private CheckBox favBtnView = null;

    private Film thisFilm = null;
    private ArrayList<Review> reviews = null;
    private ArrayList<Clip> videos = null;

    static final String DETAIL_URI = "URI";

    public DetailActivityFragment() {
    }


    private void getMovieDetails() {
        //Need to check for Internet Connectivity before attempting execute the update
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo myNet = cm.getActiveNetworkInfo();
        if (myNet == null || !myNet.isConnected()) {
            Toast.makeText(this.getActivity(),
                    "No internet detected. Please check connections.",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            FetchDetailsTask fetchDetails = new FetchDetailsTask();
            try {
                fetchDetails.execute();
            }
            catch(Exception e) {
                Toast.makeText(this.getActivity(),
                               "Unknown error occured. Please restart.",Toast.LENGTH_LONG);
            }

        }
    }

    private void setVideoContent(Context context){
        if (videos != null) {
            String trailer_template_url = "http://img.youtube.com/vi/%s/mqdefault.jpg";

            for (Clip c : videos) {
                //Traverse through each element in the list and add it to the
                //Layout

                ImageView imageView = new ImageView(context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setPadding(10, 10, 10, 10);
                imageView.setScaleType(ImageView.ScaleType.CENTER);

                //Format the trailer_url to load the image with.
                String trailer_preview_url = String.format(trailer_template_url, c.key);

                Picasso.with(context)
                        .load(trailer_preview_url)
                        .error(R.drawable.sample_3)
                        .into(imageView);

                //Store the Clip object with this ImageView
                imageView.setTag(c);

                //Now we need to set the onclickListener for this ImageView
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendToYoutube(v.getContext(), (Clip) v.getTag());
                    }
                });


                //Assuming we've loaded the image correctly, now it's time to insert it into the layout

                trailersHolderView.addView(imageView);
            }
        }
    }

    private void setReviewContent(Context context){
        //Reviews
        //No adapter for the Reviews, so we need to update the UI manually
        //Not sure if this is the right way but hope it works
        if(!reviews.isEmpty()) {
            //TextView rvw_contentTemplate = (TextView) reviewSectionTop.findViewById(R.id.detail_review_content);
            //TextView rvw_authorTemplate = (TextView) reviewSectionTop.findViewById(R.id.detail_review_author);

            //Retrieve the content from reviews arraylist
            for (Review r : reviews){
                TextView rvw_content = new TextView(context);
                TextView rvw_author = new TextView(context);
                rvw_author.setPadding(0,0,0,12);
                rvw_content.setText(r.content);
                rvw_author.setText("-" + r.author);
                reviewSectionTop.addView(rvw_content);
                reviewSectionTop.addView(rvw_author);
            }
        }
    }


    private class FetchDetailsTask extends AsyncTask<Void,Void,Void>{
        private final String LOG_TAG = FetchDetailsTask.class.getSimpleName();
        final String MDB_JSON_RESULTS = "results";

        @Override
        protected Void doInBackground(Void... params) {
            //Build the URL to query themoviedb.org API with
            Uri.Builder details_request = new Uri.Builder();
            details_request.scheme(getString(R.string.movie_db_scheme));
            details_request.authority(getString(R.string.movie_db_authority));
            details_request.appendPath(getString(R.string.movie_db_version));
            details_request.appendPath(getString(R.string.movie_db_movie));
            details_request.appendPath(thisFilm.id);
            details_request.appendQueryParameter(getString(R.string.movie_db_api_param),
                    getString(R.string.movie_db_api_key));
            details_request.appendQueryParameter("append_to_response","trailers");

            URL url = null;

            try {
                url = new URL(details_request.build().toString() +  ",reviews");
                //appended reviews parameter
                //this is pretty horrendous but I coudln't figure out a way to make
                //it work with the uri builder since there are multiple values
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG,"URL Creation ERROR",e);
            }

            //Make the call
            String api_response = executeAPICall(url);

            //Parse the API response
            //Extract extra movie details
            //Extract movie reviews
            reviews = parseReviews(api_response);
            //Extract movie trailers
            videos = parseTrailer(api_response);

            return null;
        }

        private ArrayList parseTrailer(String response) {
            //JSON parsing constants
            final String MDB_JSON_TRAILERS = "trailers";
            final String MDB_JSON_TRAILER_SOURCE = "youtube";
            final String MDB_JSON_TRAILER_NAME = "name";
            final String MDB_JSON_TRAILER_KEY = "source";
            final String MDB_JSON_TRAILER_TYPE = "type";

            //Results array
            ArrayList<Clip> clipList = new ArrayList<>();

            //Get a JSON object from response
            try {
                //Get a JSON object from response
                JSONObject movieResponse = new JSONObject(response);
                //Get the trailers object
                JSONObject trailersObj = movieResponse.getJSONObject(MDB_JSON_TRAILERS);
                //Get the JSON Array for youtube
                JSONArray youtubeArray = trailersObj.getJSONArray(MDB_JSON_TRAILER_SOURCE);
                //Create a new Clip entry for each video
                if (youtubeArray.length() > 0) {
                    for (int i = 0; i < youtubeArray.length(); i++) {
                        JSONObject videoClip = youtubeArray.getJSONObject(i);
                        //Create a new Clip object to save this response data
                        Clip clipEntry = new Clip();
                        clipEntry.key = videoClip.getString(MDB_JSON_TRAILER_KEY);
                        clipEntry.name = videoClip.getString(MDB_JSON_TRAILER_NAME);
                        clipEntry.type = videoClip.getString(MDB_JSON_TRAILER_TYPE);
                        clipList.add(clipEntry);
                    }

                }

            }catch (Exception e) {
                Log.e(LOG_TAG,"Trouble with api response: " + e);
            }

            return clipList;
        }

        private ArrayList parseReviews(String response) {
            //JSON parsing constants

            final String MDB_JSON_REVIEWS = "reviews";
            final String MDB_JSON_AUTHOR = "author";
            final String MDB_JSON_CONTENT = "content";
            final String MDB_JSON_URL = "url";

            ArrayList<Review> apiReviews = new ArrayList<>();

            try {
                JSONObject movieResponse = new JSONObject(response);
                JSONObject apiReview = movieResponse.getJSONObject(MDB_JSON_REVIEWS);
                JSONArray reviewsArray = apiReview.getJSONArray(MDB_JSON_RESULTS);

                if (reviewsArray.length() > 0) {
                    for (int i = 0; i < reviewsArray.length(); i++) {
                        JSONObject reviewObject = reviewsArray.getJSONObject(i);
                        Review review = new Review();
                        review.author = reviewObject.getString(MDB_JSON_AUTHOR);
                        review.content = reviewObject.getString(MDB_JSON_CONTENT);
                        review.url = reviewObject.getString(MDB_JSON_URL);
                        apiReviews.add(review);
                    }
                }

            } catch (Exception e) {
                Log.e(LOG_TAG,"Trouble with api response: " + e);
            }

            return apiReviews;
        }

        private String executeAPICall(URL url) {
            String movieData = null;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try{

                // Create the request to the API, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    movieData = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    movieData = null;
                }
                else {
                    movieData = buffer.toString();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attempting
                // to parse it.
                movieData = null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return movieData;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //Back on the UI thread

            //We should have videos and reviews now, if there were any.


            //Set the context
            Context context = topView.getContext();
            //Check to see if there are any videos to set up
            if (videos != null) {
                //Videos should be placed into the Video Container Layout
                setVideoContent(context);

            }

            if (reviews != null) {
                setReviewContent(context);
            }
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Initiate calls to get reviews from the API server
        //Get the trailer from the server
        if (savedInstanceState != null) {
            //recover the selected film from Bundle
            thisFilm = savedInstanceState.getParcelable(getString(R.string.detail_film_object));

            //Recover additional details fetched from the API
            ArrayList<Clip> savedVids = savedInstanceState.getParcelableArrayList(getString(R.string.detail_fetched_vids));
            ArrayList<Review> savedReviews = savedInstanceState.getParcelableArrayList(getString(R.string.detail_fetched_reviews));

            if (savedVids != null && savedReviews != null) {
                videos = savedVids;
                reviews = savedReviews;
            }
            else {
                //Somehow we saved an null list
                videos = new ArrayList<>();
                //initialize the reviews array too
                reviews = new ArrayList<>();

                if (thisFilm != null) {
                    getMovieDetails();
                }
                else {
                    Log.i("Details:onCreate","Somehow we don't have a movie. We should probably kill ourselves.");
                }

            }
        }
        else {
            //this is the first time we're launched
            //need to retrieve the Film object from the intent that launched us.
            //But how did we get launched?
            //If we're in two pane mode, the we got launched via args Bundle
            Bundle args = getArguments();
            if (args == null) {
                //We got launched via Intent
                Intent intent = getActivity().getIntent();
                if (intent.hasExtra(getString(R.string.detail_film_object))) {
                    thisFilm = intent.getExtras().getParcelable(getString(R.string.detail_film_object));

                }
            } else {
                //We got launched from the Main Activity
                //Retrieve the film from the Bundle
                if (args.containsKey(getString(R.string.detail_film_object))) {
                    thisFilm = args.getParcelable(getString(R.string.detail_film_object));
                }
            }

            //Now that we have our film, let's grab the other info from the api

            //initialize the clips array
            videos = new ArrayList<>();

            //initialize the reviews array too
            reviews = new ArrayList<>();

            if (thisFilm != null) {
                getMovieDetails();
            } else {
                Log.i("Details:onCreate", "Film intent was null! Oh no!");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        topView = (LinearLayout) rootView.findViewById(R.id.detail_layout_top);

        posterView = (ImageView) rootView.findViewById(R.id.film_detail_poster);
        titleTextView = (TextView) rootView.findViewById(R.id.film_title);
        synopsisTextView = (TextView) rootView.findViewById(R.id.film_synopsis);
        releaseView = (TextView) rootView.findViewById(R.id.film_release);
        ratingView = (TextView) rootView.findViewById(R.id.film_rating);
        favBtnView = (CheckBox) rootView.findViewById(R.id.detail_button_fav);
        reviewSectionTop = (LinearLayout) rootView.findViewById(R.id.review_layout_top);
        trailersHolderView = (LinearLayout) rootView.findViewById(R.id.detail_trailers_container);


        if (thisFilm != null) {
            //Set the status of the CheckBox to whatever the favorites member is
            favBtnView.setChecked(thisFilm.favorite);


            //Set up a listener for the CheckBox item
            favBtnView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = settings.edit();
                    HashSet<String> favs = (HashSet) settings.getStringSet(getString(R.string.pref_fav_key), new HashSet<String>());

                    if (isChecked) {
                        //We want to set this movie as a favorite
                        thisFilm.favorite = true;
                        favs.add(thisFilm.id);
                        Toast toast = new Toast(buttonView.getContext()).
                                           makeText(buttonView.getContext(),
                                           "Added to Favorites",Toast.LENGTH_SHORT);
                        toast.show();

                    } else {
                        //We want to remove this movie from our favorites
                        favs.remove(thisFilm.id);
                        thisFilm.favorite = false;

                    }

                    editor.putStringSet(getString(R.string.pref_fav_key), favs).commit();
                }

            });


            titleTextView.setText(thisFilm.title);
            synopsisTextView.setText(thisFilm.synopsis);
            releaseView.setText(thisFilm.release_date);
            ratingView.setText(thisFilm.vote_avg.toString() + "/10");

            //Load up the Poster
            Picasso.with(rootView.getContext()).load(thisFilm.poster_path).into(posterView);

            //See if we can set up Trailer and Review content from retrieved Bundle data
            if (!videos.isEmpty()) {
                setVideoContent(rootView.getContext());
            }

            if (!reviews.isEmpty()) {
                setReviewContent(rootView.getContext());
            }

        }

        return rootView;
    }

    public void sendToYoutube(Context context, Clip video) {
        Intent videoIntent = new Intent();
        Uri videoURL = null;
        try {
            Uri.Builder trailerPath = new Uri.Builder();
            videoURL = trailerPath.scheme("http")
                                  .authority("www.youtube.com")
                                  .appendPath("watch")
                                  .appendQueryParameter("v",video.key)
                                  .build();
        }catch (Exception e){
            Log.e("sendToU: ", "could not build trailer Uri " + e);
        }

        videoIntent.setAction(Intent.ACTION_VIEW);
        videoIntent.setData(videoURL);
        if(videoIntent.resolveActivity(context.getPackageManager()) != null) {
            startActivity(videoIntent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Preparing to be destroyed
        //Save this movie
        outState.putParcelable(getString(R.string.detail_film_object),thisFilm);

        //Save the videos list
        outState.putParcelableArrayList(getString(R.string.detail_fetched_vids),videos);

        //Save the reviews List
        outState.putParcelableArrayList(getString(R.string.detail_fetched_reviews), reviews);
    }
}
