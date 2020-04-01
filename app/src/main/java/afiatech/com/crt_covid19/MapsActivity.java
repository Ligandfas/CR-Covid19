package afiatech.com.crt_covid19;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;

import java.security.Permission;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private FirebaseAuth auth ;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db ;
    //**location**
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // = 5 seconds
    private Location location ;
    private LocationRequest locationRequest;
    //**Location End**


    private int FIEVRE,TOUX,EXPOSITION,MAUX_GAURGE,NAUSEE,M_CHRONIQUES,i,SCORE,TEL;
    private double LONGITUDE,LATITUDE ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //**Intances
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        db= FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //**Profil
        ImageView profil = (ImageView) findViewById(R.id.profilPic);
        if (auth.getCurrentUser()!=null) Glide.with(this).load(auth.getCurrentUser().getPhotoUrl()).into(profil);
        profil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            logout();
            }
        });

        //**Location
        // we add permissions we need to request location of the users
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }

        // we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser()!=null){
            if (!checkPlayServices()) {
                Toast.makeText(this, "You need to install Google Play Services to use the App properly", Toast.LENGTH_SHORT).show();
            }
            launchSurvey();

        }else {
            Intent intent = new Intent(this,login.class);
            startActivity(intent);
        }

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng ka = new LatLng(35.724357, 10.578294);
        LatLng kz = new LatLng(35.621065088596424, 9.089410407627753);
        LatLng ke = new LatLng(35.55405724647636, 8.908135993565253);
        LatLng kr = new LatLng(35.43553975760619, 7.685906989659004);
        LatLng kt = new LatLng(35.73262013410569, 8.869683845127753);
        LatLng kf = new LatLng(35.73262013410569, 9.380548102940253);
         mMap.addCircle(new CircleOptions()
                .center(new LatLng(35.724357, 10.578294))
                .radius(200)
                .strokeWidth(10)
                //.strokeColor(Color.GREEN)
                .fillColor(Color.argb(128, 255, 0, 0))
                );



       // mMap.addMarker(new MarkerOptions().position(ka));
        mMap.addMarker(new MarkerOptions().position(kz));
        mMap.addMarker(new MarkerOptions().position(ke));
        mMap.addMarker(new MarkerOptions().position(kr));
        mMap.addMarker(new MarkerOptions().position(kt));
        mMap.addMarker(new MarkerOptions().position(kf));

        CameraUpdate zoom=CameraUpdateFactory.zoomTo(16);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(ka));
        mMap.animateCamera(zoom);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient != null  &&  googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, (com.google.android.gms.location.LocationListener) this);
            googleApiClient.disconnect();
        }
    }

    //** login methods
    private void logout(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Déconnexion");
        builder.setMessage("Voulez vous vraiment vous déconnecter ?");
        builder.setPositiveButton("oui", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                signOut();
            }
        });
        builder.setNegativeButton("non", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void signOut() {
        auth.signOut();
        LoginManager.getInstance().logOut();
        Intent intent = new Intent(this,login.class);
        startActivity(intent);
    }
    //**login methods finished

    //**Survey methods
    private void surveyOn(String title , String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle( "استفتاء "+title)
                .setMessage(message)
                .setPositiveButton("نعم", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(i){
                            case 0 : FIEVRE=1;i++;SCORE=SCORE+2;break;
                            case 1 : TOUX=1;i++;SCORE=SCORE+2;break;
                            case 2 : EXPOSITION=1;i++;SCORE=SCORE+2;break;
                            case 3 : MAUX_GAURGE=1;i++;SCORE=SCORE+1;break;
                            case 4 : NAUSEE=1;i++;SCORE=SCORE+1;break;
                            case 5 : M_CHRONIQUES=1;i++;SCORE=SCORE+1;break;
                        }
                    }
                })
                .setNegativeButton("لا", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(i){
                            case 0 : FIEVRE=0;i++;break;
                            case 1 : TOUX=0;i++;;break;
                            case 2 : EXPOSITION=0;;break;
                            case 3 : MAUX_GAURGE=0;;break;
                            case 4 : NAUSEE=0;i++;;break;
                            case 5 : M_CHRONIQUES=0;i++;;break;
                        }
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();


    }
    private void showTelDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("رقم الهاتف");
        builder.setMessage("الرجاء ادخار رقم الهاتف للاتصال عند الحاجة");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setPadding(20,0,20,0);
        builder.setView(input);
        builder.setCancelable(false);

        builder.setPositiveButton("تأكيد", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                uploadData();
                if (input.getText()!=null&&!input.getText().toString().equals("")){
                    TEL=Integer.valueOf(input.getText().toString());
                    if (SCORE<4){
                        showRslt("احتمال اصابتك بالفيروس ضئيلة جدا الرجاء الالتزام بالحجر الصحي العام لتفادي العدوى لك و لعائلتك",android.R.attr.checked) ;
                    }
                    else showRslt("احتمال اصابتك بالفيروس واردة الرجاء الالتزام بالحجر الذاتي و الاتصال ب 190 للاطمئنان على صحتك",android.R.attr.alertDialogIcon);


                }else dialog.cancel();


            }
        });

        builder.setNegativeButton("لا", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                uploadData();
                dialog.cancel();
            }
        });

        builder.show();
    }
    private void showRslt(String message, int attr){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("النتيجة")
                .setIconAttribute(attr)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("شكرا",null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void uploadData(){
        Map<String, Object> user = new HashMap<>();
        user.put("id", auth.getCurrentUser().getUid());
        user.put("nom", auth.getCurrentUser().getDisplayName());
        user.put("email", auth.getCurrentUser().getEmail());
        user.put("photo", auth.getCurrentUser().getPhotoUrl()+"");
        user.put("tel", TEL);
        user.put("lat", LATITUDE+"");
        user.put("lent",LONGITUDE+"");
        user.put("score",SCORE);
        user.put("exposition",EXPOSITION);
        user.put("fièvre",FIEVRE);
        user.put("toux/Dyspnee",TOUX);
        user.put("nausee/vomi/diaree",NAUSEE);
        user.put("MChronique",M_CHRONIQUES);
        user.put("MAUX_GORGE",MAUX_GAURGE);



        db.collection("users").document(auth.getUid()).set(user, SetOptions.merge());
    }
    private void launchSurvey(){
        i=0;
        SCORE=0;
        showTelDialog();
        surveyOn(" 6/6 ","هل لديك احدى الأمراض المزمنة التالية: القلب أو التنفس أو الكلى ؟");
        surveyOn(" 5/6 ","هل لديك احساس بالغثيان أو التقيؤ أو الإسهال ؟");
        surveyOn(" 4/6 ","هل لديك إلتهاب في الحلق ؟");
        surveyOn(" 3/6 ","هل تعاملت مع شخص يحمل الفيروس ؟");
        surveyOn(" 2/6 ","هل لديك سعال جاف أو ضيق في التنفس ؟");
        surveyOn(" 1/6 ","هل لديك حمى ؟");
    }
    //**Survey methods END**

    //**Location methods
    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }
    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }
                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }

                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                    }
                }

                break;
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }

            return false;
        }

        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Toast.makeText(this, "Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            LONGITUDE=location.getLongitude();
            LATITUDE=location.getLatitude() ;
            Toast.makeText(this, "Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

       // LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, getApplicationContext());
    }

    //**Location methods END**
}
