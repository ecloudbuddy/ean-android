package com.ean.mobile.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import com.ean.mobile.Destination;
import com.ean.mobile.HotelInfo;
import com.ean.mobile.R;
import com.ean.mobile.SampleApp;
import com.ean.mobile.SampleConstants;
import com.ean.mobile.exception.EanWsError;
import com.ean.mobile.exception.UrlRedirectionException;
import com.ean.mobile.request.DestLookup;
import com.ean.mobile.request.ListRequest;
import com.ean.mobile.task.SuggestionFactory;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StartupSearch extends Activity {

    private static final String DATE_FORMAT_STRING = "MM/dd/yyyy";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT_STRING);

    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.startupsearch);

        // get the current date
        LocalDate today = LocalDate.now();
        SampleApp.arrivalDate = today;
        SampleApp.departureDate = today.plusDays(1);


        Button arrivalDate = (Button) findViewById(R.id.arrival_date_picker);
        Button departureDate = (Button) findViewById(R.id.departure_date_picker);
        // set the date on the button
        arrivalDate.setText(DATE_TIME_FORMATTER.print(SampleApp.arrivalDate));
        departureDate.setText(DATE_TIME_FORMATTER.print(SampleApp.departureDate));

        setUpPeopleSpinner(R.id.adults_spinner);
        setUpPeopleSpinner(R.id.children_spinner);

        final ArrayAdapter<Destination> suggestionAdapter = new DestinationSuggestionAdapter(this, R.id.suggestionsView);
        final SearchBoxTextWatcher watcher = new SearchBoxTextWatcher(suggestionAdapter);

        final EditText searchBox = (EditText) findViewById(R.id.searchBox);
        searchBox.setOnKeyListener(new SearchBoxKeyListener(searchBox));
        searchBox.addTextChangedListener(watcher);

        final ListView suggestions = (ListView) findViewById(R.id.suggestionsView);
        suggestions.setAdapter(suggestionAdapter);
        suggestions.setOnItemClickListener(new SuggestionListAdapterClickListener(searchBox, watcher));
    }

    private void setUpPeopleSpinner(int resourceId) {
        final ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.number_of_people_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final Spinner spinner = (Spinner) findViewById(resourceId);
        spinner.setAdapter(spinnerAdapter);
    }

    public void showDatePickerDialog(View view) {
        new DatePickerFragment(view.getId(), (Button) view).show(getFragmentManager(), "StartupSearchDatePicker");
    }

    private void performSearch(final String searchQuery, final ProgressDialog searchingDialog) {
        SuggestionFactory.killSuggestDestinationTask();
        SampleApp.clearSearch();

        Button arrivalDatePicker = (Button) findViewById(R.id.arrival_date_picker);
        Button departureDatePicker = (Button) findViewById(R.id.departure_date_picker);
        Spinner adultsSpinner = (Spinner) findViewById(R.id.adults_spinner);
        Spinner childSpinner = (Spinner) findViewById(R.id.adults_spinner);

        SampleApp.searchQuery = searchQuery;
        SampleApp.arrivalDate = DATE_TIME_FORMATTER.parseLocalDate(arrivalDatePicker.getText().toString());
        SampleApp.departureDate = DATE_TIME_FORMATTER.parseLocalDate(departureDatePicker.getText().toString());
        SampleApp.numberOfAdults = Integer.parseInt((String) adultsSpinner.getSelectedItem());
        SampleApp.numberOfChildren = Integer.parseInt((String) childSpinner.getSelectedItem());
        
        new PerformSearchTask(searchingDialog).execute((Void) null);
    }

    private class PerformSearchTask extends AsyncTask<Void, Integer, Void> {

        private final ProgressDialog searchingDialog;

        private PerformSearchTask(final ProgressDialog searchingDialog) {
            this.searchingDialog = searchingDialog;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                SampleApp.foundHotels = ListRequest.searchForHotels(SampleApp.searchQuery, SampleApp.occupancy(), SampleApp.arrivalDate, SampleApp.departureDate, null, SampleApp.LOCALE.toString(), SampleApp.CURRENCY.toString());
            } catch (IOException e) {
                Log.d(SampleConstants.DEBUG, "An IOException occurred while searching for hotels.", e);
            } catch (EanWsError ewe) {
                //TODO: This should be handled better. If this exception occurs, it's likely an input error and should be recoverable.
                Log.d(SampleConstants.DEBUG, "An APILevel Exception occurred.", ewe);
            } catch (UrlRedirectionException ure) {
                SampleApp.sendRedirectionToast(getApplicationContext());
            }
            return null;
         }

        @Override
        protected void onPostExecute(Void aLong) {
            Intent intent = new Intent(StartupSearch.this, HotelList.class);
            startActivity(intent);
            try {
                searchingDialog.dismiss();
            } catch (IllegalArgumentException iae) {
                // just ignore it... it's because the window rotated at an inopportune time.
            }
        }

        @Override
        protected void onCancelled() {
            searchingDialog.dismiss();
        }
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private final int pickerId;

        private final Button pickerButton;

        public DatePickerFragment(final int pickerId, final Button pickerButton) {
            super();
            this.pickerId = pickerId;
            this.pickerButton = pickerButton;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final LocalDate date;
            if (pickerId == R.id.arrival_date_picker) {
                date = SampleApp.arrivalDate;
            } else {
                date = SampleApp.departureDate;
            }

            return new DatePickerDialog(getActivity(), this, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
        }

        @Override
        public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
            LocalDate chosenDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
            pickerButton.setText(DATE_TIME_FORMATTER.print(chosenDate));
            switch(pickerId) {
                case R.id.arrival_date_picker:
                    SampleApp.arrivalDate = chosenDate;
                    break;
                case R.id.departure_date_picker:
                    SampleApp.departureDate = chosenDate;
                    break;
                default:
                    break;
            }
        }
    }

    private class SuggestionListAdapterClickListener implements AdapterView.OnItemClickListener {

        private final EditText searchBox;

        private final TextWatcher textWatcher;

        public SuggestionListAdapterClickListener(final EditText searchBox, final TextWatcher textWatcher) {
            this.searchBox = searchBox;
            this.textWatcher = textWatcher;
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //replace the text in the search box with the string at position.
            Destination selectedDestination = (Destination) parent.getItemAtPosition(position);
            //disable the text change listener.
            searchBox.removeTextChangedListener(textWatcher);
            //set the text.
            searchBox.setText(selectedDestination.name);
            //clear the suggestions
            ArrayAdapter suggestionAdapter = (ArrayAdapter) parent.getAdapter();
            suggestionAdapter.clear();
            suggestionAdapter.notifyDataSetChanged();
            //re-enable the change listener.
            searchBox.addTextChangedListener(textWatcher);
        }
    }

    private class SearchBoxKeyListener implements View.OnKeyListener {
        private final EditText searchBox;

        public SearchBoxKeyListener(final EditText searchBox) {
            this.searchBox = searchBox;
        }

        public boolean onKey(View v, int keyCode, KeyEvent event) {
            // Only hears hardware keys and enter per OnKeyListener's javadoc.
            // If the event is a key-down event on the "enter" button
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // Perform action on key press
                performSearch(searchBox.getText().toString().trim(), ProgressDialog.show(StartupSearch.this, "", getString(R.string.searching), true));
                return true;
            }
            return false;
        }
    }

    private class SearchBoxTextWatcher implements TextWatcher {

        private final ArrayAdapter<Destination> suggestionAdapter;

        private SearchBoxTextWatcher(ArrayAdapter<Destination> suggestionAdapter) {
            this.suggestionAdapter = suggestionAdapter;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

        @Override
        public void afterTextChanged(Editable editable) {
            SuggestionFactory.suggest(editable.toString(), suggestionAdapter);
        }
    }

    private static class DestinationSuggestionAdapter extends ArrayAdapter<Destination> {
        private final LayoutInflater layoutInflater;

        private DestinationSuggestionAdapter(Context context, int resource) {
            super(context, resource, new ArrayList<Destination>());
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * {@inheritDoc}
         */
        public View getView (int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = layoutInflater.inflate(R.layout.destinationlistlayout, null);
            }

            final Destination destination = this.getItem(position);

            //Set the name field
            final TextView name = (TextView) view.findViewById(R.id.destinationSuggestionName);
            name.setText(destination.name);

            return view;
        }
    }
}
