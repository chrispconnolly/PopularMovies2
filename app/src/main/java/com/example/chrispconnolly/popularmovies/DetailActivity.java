package com.example.chrispconnolly.popularmovies;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {
    private String mMovieId;
    private ArrayAdapter<String> mReviewsAdapter, mTrailersAdapter;
    private ArrayList<String> mReviewsList, mTrailersList;
    private ListView mReviewsListView, mTrailersListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        try {
            Bitmap bitmap = this.getIntent().getParcelableExtra("movieBitmap");
            ImageView posterImageView = (ImageView) this.findViewById(R.id.poster_imageview);
            posterImageView.setImageBitmap(bitmap);

            TextView titleTextView = (TextView) this.findViewById(R.id.title_textview);
            TextView plotTextView = (TextView) this.findViewById(R.id.plot_textview);
            TextView ratingTextView = (TextView) this.findViewById(R.id.rating_textview);
            TextView releaseDateTextView = (TextView) this.findViewById(R.id.releasedate_textview);

            JSONObject movieJson = new JSONObject(this.getIntent().getExtras().getString("movieJson"));
            mMovieId = movieJson.getString("id");
            String favorites = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("favorites", null);
            ToggleButton favoriteToggleButton = (ToggleButton)this.findViewById(R.id.favorite_togglebutton);
            favoriteToggleButton.setChecked(favorites != null && favorites.contains(mMovieId));

            titleTextView.setText("Original Title: " + movieJson.getString("original_title"));
            plotTextView.setText("Plot: " + movieJson.getString("overview"));
            ratingTextView.setText("User Rating: " + movieJson.getString("vote_average"));
            releaseDateTextView.setText("Release Date: " + movieJson.getString("release_date"));

            //Load Trailers
            mTrailersListView = (ListView) this.findViewById(R.id.trailers_listview);
            mTrailersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/watch?v=" + mTrailersList.get(position)));
                    startActivity(intent);
                }
            });
            mTrailersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            mTrailersListView.setAdapter(mTrailersAdapter);
            FetchTrailersTask fetchMoviesTask = new FetchTrailersTask();
            fetchMoviesTask.execute();

            //Load Reviews
            mReviewsListView = (ListView) this.findViewById(R.id.reviews_listview);
            mReviewsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(DetailActivity.this);
                    alertDialogBuilder.setMessage(mReviewsList.get(position));
                    alertDialogBuilder.create().show();
                }
            });
            mReviewsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            mReviewsListView.setAdapter(mReviewsAdapter);
            FetchReviewsTask fetchReviewsTask = new FetchReviewsTask();
            fetchReviewsTask.execute();

            ((ScrollView)this.findViewById(R.id.details_scrollview)).smoothScrollTo(0, 0);
        }
        catch (JSONException e)
        {
            Log.e("JSON Exception: ", e.toString());
        }
    }

    public void markAsFavorite(View view){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String favorites = sharedPreferences.getString("favorites", null);
        ToggleButton favoriteToggleButton = (ToggleButton)view;
        favorites = (favorites == null) ? "" : favorites;
        if(favoriteToggleButton.isChecked())
            favorites += mMovieId + ",";
        else
            favorites = favorites.replace(mMovieId + ",", "");
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putString("favorites", favorites);
        preferencesEditor.commit();
    }

    public class FetchTrailersTask extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected void onPostExecute(JSONObject movieTrailersJson) {
            //Load into ListView
            try{
                System.out.println(movieTrailersJson);
                mTrailersList = new ArrayList<>();
                mTrailersAdapter.clear();
                JSONArray trailersJsonArray = movieTrailersJson.getJSONArray("results");
                List<String> trailersList = new ArrayList<>();
                for (int i = 0; i < trailersJsonArray.length(); i++) {
                    JSONObject jsonObject = trailersJsonArray.getJSONObject(i);
                    if(jsonObject.getString("site").equals("YouTube")){
                        trailersList.add(jsonObject.getString("name"));
                        mTrailersList.add(jsonObject.getString("key"));
                    }
                }
                for (String video : trailersList)
                    mTrailersAdapter.add("Watch " + video);
                View listItem = mTrailersAdapter.getView(0, null, mTrailersListView);
                listItem.measure(0, 0);
                int height = listItem.getMeasuredHeight()*mTrailersAdapter.getCount();
                mTrailersListView.getLayoutParams().height = height;
            }
            catch (Exception exception){
                Log.e("JSON Exception", exception.toString());
            }
        }
        @Override
        protected JSONObject doInBackground(String... params) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("api.themoviedb.org")
                    .appendPath("3")
                    .appendPath("movie")
                    .appendPath(mMovieId)
                    .appendPath("videos")
                    .appendQueryParameter("api_key", BuildConfig.THE_MOVIE_DATABASE_API_KEY);
            return Utility.getMovieJson(builder.build().toString());
        }
    }

    public class FetchReviewsTask extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected void onPostExecute(JSONObject movieReviewsJson) {
            try {
                mReviewsList = new ArrayList<>();
                mReviewsAdapter.clear();
                JSONArray reviewsJsonArray = movieReviewsJson.getJSONArray("results");
                List<String> reviewsList = new ArrayList<>();
                for (int i = 0; i < reviewsJsonArray.length(); i++) {
                    JSONObject jsonObject = reviewsJsonArray.getJSONObject(i);
                    reviewsList.add(jsonObject.getString("author"));
                    mReviewsList.add(jsonObject.getString("content"));
                }
                for (String author : reviewsList)
                    mReviewsAdapter.add("Read a review by " + author);
                View listItem = mReviewsAdapter.getView(0, null, mReviewsListView);
                listItem.measure(0, 0);
                int height = listItem.getMeasuredHeight()*mReviewsAdapter.getCount();
                mReviewsListView.getLayoutParams().height = height;
            }
            catch (Exception exception){
                Log.e("JSON Exception", exception.toString());
            }
        }
        @Override
        protected JSONObject doInBackground(String... params) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("api.themoviedb.org")
                    .appendPath("3")
                    .appendPath("movie")
                    .appendPath(mMovieId)
                    .appendPath("reviews")
                    .appendQueryParameter("api_key", BuildConfig.THE_MOVIE_DATABASE_API_KEY);
            return Utility.getMovieJson(builder.build().toString());
        }
    }

}
