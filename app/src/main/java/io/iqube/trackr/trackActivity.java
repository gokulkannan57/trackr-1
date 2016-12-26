package io.iqube.trackr;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.client.Firebase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class trackActivity extends FragmentActivity implements OnMapReadyCallback,MqttCallback {


    private static GoogleMap mMap;



     private Firebase mRef;

    int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;

    ImageButton locationBus,select,about;

    LatLng latLng;

    LatLng default_latng;

    String bus_no;

    Spinner select_bus;

    Button track;

    MqttAndroidClient client;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_track);

        mqttReg();

        locationBus = (ImageButton)findViewById(R.id.location_bus);

        select = (ImageButton)findViewById(R.id.select);

        about = (ImageButton)findViewById(R.id.about);


        final SharedPreferences bus = getSharedPreferences("bus_no", MODE_PRIVATE);

        statusCheck();
        isOnline();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        locationBus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        });

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              busDialog(1);
            }
        });

        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aboutDialog();
            }
        });

        if(bus.getString("bus_no",null)==""||bus.getString("bus_no",null)==null)
        {
            busDialog(0);
        }

    }

  private void createMarker() {


        MarkerOptions a = new MarkerOptions().title("Your Bus").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)).flat(true).position(new LatLng(11.077376, 76.98866));

        final Marker m = mMap.addMarker(a);


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {

            ActivityCompat.requestPermissions(trackActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

       mRef = new Firebase("https://trackr-1477046660127.firebaseio.com/Location");

        mRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                Map map = snapshot.getValue(Map.class);
                Double Lat = Double.parseDouble(map.get("lat").toString());
                Double Lng = Double.parseDouble(map.get("lng").toString());


                latLng = new LatLng(Lat, Lng);

                m.setPosition(latLng);

               // Toast.makeText(trackActivity.this,map.toString(),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }




    @Override
    public void onMapReady(GoogleMap map) {

        default_latng = new LatLng(11.077376,76.98866);

        mMap = map;

        mMap.moveCamera(CameraUpdateFactory.newLatLng(default_latng));

        mMap.animateCamera(CameraUpdateFactory.zoomTo(10));

        UiSettings UiSettings = map.getUiSettings();
        UiSettings.setZoomControlsEnabled(true);

        createMarker();


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            if (permissions.length == 1 &&
                    permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(trackActivity.this, "Permssion Denied", Toast.LENGTH_SHORT).show();
            }
        }

    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void busDialog(int checkCode){

        final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.select_bus);

        final SharedPreferences bus = getSharedPreferences("bus_no", MODE_PRIVATE);

        final SharedPreferences.Editor editor = bus.edit();

        if(checkCode==0)
        {
            dialog.setCancelable(false);
        }
        else if(checkCode==1)
        {
            dialog.setCancelable(true);
        }

        dialog.show();

        select_bus = (Spinner)dialog.findViewById(R.id.spinner);

        track = (Button)dialog.findViewById(R.id.getSelected);

        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    bus_no = select_bus.getSelectedItem().toString();
                }
                catch (NullPointerException e)
                {
                    bus_no = "1";
                }

                editor.putString("bus_no",bus_no);
                editor.commit();

                dialog.hide();

                Toast.makeText(trackActivity.this,bus.getString("bus_no",null),Toast.LENGTH_SHORT).show();

            }
        });


    }

    private void aboutDialog(){

        final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.about);

        dialog.show();

    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        else
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Your Internet seems to be disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }
        return false;
    }

    public void mqttReg()
    {
        String clientId = MqttClient.generateClientId();
        client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883",
                        clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(trackActivity.this,"Success",Toast.LENGTH_SHORT).show();
                    sub();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(trackActivity.this,"Failure",Toast.LENGTH_SHORT).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();

        }
    }

    public void sub()
    {
        String topic = "trackrkct";
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            client.setCallback(this);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(trackActivity.this,"Success Subscribing",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(trackActivity.this,"Failure Subscribing",Toast.LENGTH_SHORT).show();

                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }



    }


    @Override
    public void connectionLost(Throwable cause) {
        Toast.makeText(trackActivity.this,"ConnectionLost",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        String gpsData = message.toString();

        JSONObject jsonGps = new JSONObject(gpsData);

        String lat =  jsonGps.get("lat").toString();
        String lng = jsonGps.get("lng").toString();
        String spd = jsonGps.get("spd").toString();
        String time = jsonGps.get("time").toString();


        Toast.makeText(trackActivity.this,lat+lng+spd+time,Toast.LENGTH_SHORT).show();



    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Toast.makeText(trackActivity.this,"Delivery Complete",Toast.LENGTH_SHORT).show();
    }
}
