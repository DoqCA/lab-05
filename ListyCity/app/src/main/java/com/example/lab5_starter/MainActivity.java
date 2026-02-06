package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;

    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    cityArrayList.add(new City(name, province));
                }
            }
            // Added to make sure the app list is synced with the firebase from the very start
            // In the lab we didn't do this either iirc (I could've just missed it), since I had to correct this.
            // So if you're a TA marking, and there's a lot of apps crashing this could be why lol
            cityArrayAdapter.notifyDataSetChanged();
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        addDummyData();

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });

        // Set a long click to delete implementation (easiest vs creating a button lol)
        cityListView.setOnItemLongClickListener(((adapterView, view, i, l) -> {
            City cityToDel = cityArrayAdapter.getItem(i);

            deleteCity(cityToDel);

            return true;
        }));

    }

    @Override
    public void updateCity(City city, String title, String province) {
        // The name is immutable as it serves as the id, so the object has to be deleted then recreated.
        if (!city.getName().equals(title)) {
            citiesRef.document(city.getName()).delete();

            City newCity = new City(title, province);
            citiesRef.document(title).set(newCity);
        } else { // If the province field needs to be updated, it we can just update directly since it's not the ID field.
            citiesRef.document(city.getName()).update("province", province);
        }
    }

    @Override
    public void addCity(City city){
        // commented out because listener is what updates the list on the app.
        // In the lab I don't believe we did this, and had the manual changes below because
        // This is what was left here, or it didn't save, but I had the firebase code so.
//        cityArrayList.add(city);
//        cityArrayAdapter.notifyDataSetChanged();

        // Adding city to the firebase (citiesRef).
        citiesRef.document(city.getName())
                .set(city)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Firestore", "Document added successfully");
                    }
                });

    }

    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");

        // Changed from lab, since the dummy data was manually added and crashed the app as it desynced the citieslist from the db
        // Can comment out to avoid this data getting added everytime (since we're in onCreate).
        citiesRef.document(m1.getName()).set(m1);
        citiesRef.document(m2.getName()).set(m2);
    }

    @Override
    public void deleteCity(City city) {
        citiesRef.document(city.getName())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Firestore", "City deleted successfully");
                    }
                });
    }
}