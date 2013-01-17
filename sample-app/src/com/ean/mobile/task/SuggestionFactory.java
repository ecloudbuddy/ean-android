
package com.ean.mobile.task;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import com.ean.mobile.Destination;
import com.ean.mobile.SampleConstants;
import com.ean.mobile.request.DestLookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SuggestionFactory {
    private static SuggestDestinationTask suggestDestinationTask = null;

    /**
     * Private no-op constructor to prevent instantiation.
     */
    private SuggestionFactory() {
        throw new UnsupportedOperationException("Do not use an instance of this class, instead use the factory.");
    }


    public static void killSuggestDestinationTask() {
        if (suggestDestinationTask != null && suggestDestinationTask.getStatus() == AsyncTask.Status.RUNNING) {
            suggestDestinationTask.cancel(true);
        }
    }

    public static void suggest(final String query, final ArrayAdapter<Destination> suggestionAdapter) {
        killSuggestDestinationTask();
        suggestDestinationTask = new SuggestDestinationTask(suggestionAdapter);
        suggestDestinationTask.execute(query);
    }

    private static class SuggestDestinationTask extends AsyncTask<String, Integer, List<Destination>> {

        private final ArrayAdapter<Destination> suggestionAdapter;

        private SuggestDestinationTask(ArrayAdapter<Destination> suggestionAdapter) {
            this.suggestionAdapter = suggestionAdapter;
        }

        @Override
        protected List<Destination> doInBackground(String... strings) {
            try {
                return DestLookup.getDestInfos(strings[0]);
            } catch (IOException e) {
                Log.d(SampleConstants.DEBUG, "An IOException occurred while searching for hotels.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Destination> destinations) {
            if (destinations != null) {
                List<Destination> cities = new ArrayList<Destination>();
                for (Destination destination : destinations) {
                    if (destination.category == Destination.Category.CITIES) {
                        cities.add(destination);
                        if (cities.size() > 5) {
                            break;
                        }
                    }
                }
                suggestionAdapter.clear();
                suggestionAdapter.addAll(cities);
                suggestionAdapter.notifyDataSetChanged();
            }
        }
    }
}