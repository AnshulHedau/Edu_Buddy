package com.sada.edubuddy;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

public class ConversationFragment extends Fragment implements LoaderManager.LoaderCallbacks<HashMap<String, String>>, TextToSpeech.OnInitListener {

    private static final String TAG = "ConversationFragment";
    private static final int API_LOADER_ID = 1;
    private static final int ADD_TO_FIREBASE = 5;
    private LinearLayout messagesContainer;
    private EditText etMessage;
    private ImageButton bSendMessage, bRecordMMessage;
    private ScrollView scrollView;
    final private String BASE_URL = "https://edu-buddy.herokuapp.com/query?query=";
    private String QUERY_URL;
    private boolean loaderInitiated = false;
    private boolean addingEvent = false;
    final private int DESCRIPTION = 0;
    final private int FROM = 2;
    final private int TO = 3;
    final private int DURATION = 4;
    final private int DATE = 1;
    private int currentStep = 0;
    String description = null, from = null, to = null, duration = null, date = null;
    private TextToSpeech myTTS;
    private int MY_DATA_CHECK_CODE = 0;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean firstTime = true;

    public static ConversationFragment newInstance() {

        Bundle args = new Bundle();

        ConversationFragment fragment = new ConversationFragment();
        fragment.setArguments(args);
        return fragment;
    }


    public ConversationFragment() {
        // Required empty public constructor
    }

    private void initiateLoader() {
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }

        // If there is a network connection, fetch data
        if (networkInfo != null && networkInfo.isConnected()) {

            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            if (!loaderInitiated) {
                loaderManager.initLoader(API_LOADER_ID, null, this);
                loaderInitiated = true;
            } else {
                loaderManager.restartLoader(API_LOADER_ID, null, this);
            }


        } else {
            Toast.makeText(getActivity(), "No INTERNET connection", Toast.LENGTH_SHORT).show();
//            View loadingIndicator = findViewById(R.id.loading_indicator);
//            loadingIndicator.setVisibility(View.GONE);
//            avi.hide();
//
//            ((TextView) errorContainer.findViewById(R.id.tvErrorDesc)).setText(R.string.no_conn_error_message);
//            errorContainer.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_conversation, container, false);

        messagesContainer = (LinearLayout) rootView.findViewById(R.id.messagesContainer);
        etMessage = (EditText) rootView.findViewById(R.id.etMessage);
        bRecordMMessage = (ImageButton) rootView.findViewById(R.id.bRecordMessage);
        bSendMessage = (ImageButton) rootView.findViewById(R.id.bSendMessage);
        scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);

        //check for TTS data
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if (speechRecognizerIntent != null) {
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float v) {

                }

                @Override
                public void onBufferReceived(byte[] bytes) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int i) {

                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    etMessage.setText(matches.get(0));
                    if (speechRecognizer != null) {
                        speechRecognizer.stopListening();
                    }
                }

                @Override
                public void onPartialResults(Bundle bundle) {

                }

                @Override
                public void onEvent(int i, Bundle bundle) {

                }
            });
        } else {
            Log.i(TAG, "onCreateView: NULL");
        }

        bRecordMMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: RECORDING STARTED");
                speechRecognizer.startListening(speechRecognizerIntent);
            }
        });
        bSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = etMessage.getText().toString().trim();
                View messageView = null;
                if (message.length() != 0) {
                    if (addingEvent) {
                        switch (currentStep) {
                            case DESCRIPTION:
                                description = message;
                                currentStep++;
                                messageView = generateView(message, "user");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
//                                speakMessage(message);
                                message = "When is your event - the date?";
                                messageView = generateView(message, "bot");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
                                speakMessage(message);
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.scrollTo(0, scrollView.getBottom());
                                    }
                                });
                                break;
                            case DATE:
                                date = message;
                                messageView = generateView(message, "user");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
//                                speakMessage(message);
                                message = "When is your event from ?";
                                messageView = generateView(message, "bot");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
                                speakMessage(message);
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.scrollTo(0, scrollView.getBottom());
                                    }
                                });
                                currentStep++;
                                break;
                            case FROM:
                                from = message;
                                currentStep++;
                                messageView = generateView(message, "user");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
//                                speakMessage(message);
                                message = "When does your event end ?";
                                messageView = generateView(message, "bot");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
                                speakMessage(message);
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.scrollTo(0, scrollView.getBottom());
                                    }
                                });
                                break;
                            case TO:
                                to = message;
                                messageView = generateView(message, "user");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
//                                speakMessage(message);
                                message = "How much time do you think you would take to complete the task?";
                                messageView = generateView(message, "bot");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
                                speakMessage(message);
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.scrollTo(0, scrollView.getBottom());
                                    }
                                });
                                currentStep++;
                                break;
                            case DURATION:
                                duration = message;
                                messageView = generateView(message, "user");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
//                                speakMessage(message);
                                message = "That's great, Just a minute while I set-up your event...";
                                messageView = generateView(message, "bot");
                                etMessage.setText("");
                                messagesContainer.addView(messageView);
                                speakMessage(message);
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.scrollTo(0, scrollView.getBottom());
                                    }
                                });
                                currentStep++;
                                addToFirebase(description, from, to, duration, date);
                                break;
                        }
                    } else {
                        messageView = generateView(message, "user");
                        etMessage.setText("");
                        messagesContainer.addView(messageView);
//                        speakMessage(message);
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.scrollTo(0, scrollView.getBottom());
                            }
                        });
                        QUERY_URL = BASE_URL + message;
                        initiateLoader();
                    }
                }
            }
        });

        ConstraintLayout layout = new ConstraintLayout(getActivity());
        if (firstTime) {

            String message = "Hi I'm EduBuddy, Your personal assistant to make your daily workload easier and your schedule more organised";
            View messageView = generateView(message, "bot");
            messagesContainer.addView(messageView);
//            speakMessage(message);
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.scrollTo(0, scrollView.getBottom());
                }
            });

            firstTime = false;
        }

        return rootView;
    }

    private void addToFirebase(String description, String from, String to, String duration, String date) {
        Log.i(TAG, "addToFirebase: ADDING");
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        String userId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference currentSchedule = FirebaseDatabase.getInstance().getReference().child("users")
                .child(userId).child("schedule");
        String key = currentSchedule.push().getKey();
        currentSchedule.child(key).child("description").setValue(description);
        currentSchedule.child(key).child("from").setValue(from);
        currentSchedule.child(key).child("to").setValue(to);
        currentSchedule.child(key).child("duration").setValue(duration);
        currentSchedule.child(key).child("date").setValue(date);
        currentSchedule.child(key).child("icon_identifier").setValue("before");
        String message = "Your event has been updated to you timeline!!!";
        View messageView = generateView(message, "bot");
        etMessage.setText("");
        messagesContainer.addView(messageView);
        speakMessage(message);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0, scrollView.getBottom());
            }
        });
        addingEvent = false;
        currentStep = 0;
    }

    public View generateView(CharSequence message, String data_1) {
        LayoutInflater inflater1 = LayoutInflater.from(getContext());
        View view = null;
        TextView etIndividualMsg = null;
        if (data_1.equals("user")) {
            view = inflater1.inflate(R.layout.message_from_user, null);
            etIndividualMsg = (TextView) view.findViewById(R.id.userMessage);
        } else {
            view = inflater1.inflate(R.layout.message_from_bot, null);
            etIndividualMsg = (TextView) view.findViewById(R.id.botMessage);
        }
        etIndividualMsg.setText(message);
        return view;
    }

    @Override
    public Loader<HashMap<String, String>> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCreateLoader: QUERY URL = " + QUERY_URL);
        return new BotMessageLoader(getActivity(), QUERY_URL);
    }

    @Override
    public void onLoadFinished(Loader<HashMap<String, String>> loader, HashMap<String, String> data) {
        CharSequence msg = data.get("intent") + " : " + data.get("data_1") + " : " + data.get("data_2");
        Log.i(TAG, "onLoadFinished: " + msg);
//        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();

        String intent = data.get("intent");
        String data_1 = data.get("data_1");
        String data_2 = data.get("data_2");

        switch (intent) {
            case "showTimeline":
                msg = "Taking you to your Timeline";
                ViewPager viewPager = getActivity().findViewById(R.id.container);
                viewPager.setCurrentItem(1);
                break;
            case "addEvent":
                addingEvent = true;
                addEvent();
                break;
            case "getNews":
                int textSize1 = getResources().getDimensionPixelSize(R.dimen.text_size_1);
                int textSize2 = getResources().getDimensionPixelSize(R.dimen.text_size_2);

                String text1 = data_1;
                String text2 = data_2;

                SpannableString span1 = new SpannableString(text1);
                span1.setSpan(new AbsoluteSizeSpan(textSize1), 0, text1.length(), SPAN_INCLUSIVE_INCLUSIVE);

                SpannableString span2 = new SpannableString(text2);
                span2.setSpan(new AbsoluteSizeSpan(textSize2), 0, text2.length(), SPAN_INCLUSIVE_INCLUSIVE);

                msg = TextUtils.concat(span1, "\n", span2);
                break;
            case "foundError":
                msg = "Sorry I did not understand that!!!";
                break;
        }
        if (!addingEvent) {
            View messageView = generateView(msg, "bot");
            etMessage.setText("");
            messagesContainer.addView(messageView);
            speakMessage(msg.toString());
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.scrollTo(0, scrollView.getBottom());
                }
            });
        }

    }

    private void addEvent() {
        String message = "Can you please help me with the event description";
        View messageView = generateView(message, "bot");
        etMessage.setText("");
        messagesContainer.addView(messageView);
        speakMessage(message);
    }


    @Override
    public void onLoaderReset(Loader<HashMap<String, String>> loader) {

    }

    //speak the user text
    private void speakMessage(String speech) {

        //speak straight away
        myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
    }

    //act on result of TTS data check
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                //the user has the necessary data - create the TTS
                myTTS = new TextToSpeech(getActivity(), this);
            } else {
                //no data - install it now
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }
    }

    //setup TTS
    public void onInit(int initStatus) {

        //check for successful instantiation
        if (initStatus == TextToSpeech.SUCCESS) {
            if (myTTS.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
                myTTS.setLanguage(Locale.US);
        } else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(getActivity(), "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }
    }
}
