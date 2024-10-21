package com.crossbowffs.nekosms.app;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.widget.StringAdapter;
import com.crossbowffs.nekosms.widget.OnItemSelectedListenerAdapter;
import com.crossbowffs.nekosms.widget.TextWatcherAdapter;
import com.google.android.material.textfield.TextInputLayout;

import java.util.LinkedHashMap;

public class FilterEditorFragment extends Fragment {
    public static final String EXTRA_FIELD = "field";

    private SmsFilterField mField;
    private TextInputLayout mPatternTextInputLayout;
    private EditText mPatternEditText;
    private Spinner mModeSpinner;
    private Spinner mCaseSensitiveSpinner;
    private StringAdapter<SmsFilterMode> mModeAdapter;
    private StringAdapter<Boolean> mCaseSensitiveAdapter;
    private SmsFilterPatternData mPatternData;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mField = (SmsFilterField)getArguments().getSerializable(EXTRA_FIELD);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pattern_editor, container, false);
        mPatternTextInputLayout = view.findViewById(R.id.filter_editor_pattern_inputlayout);
        mPatternEditText = view.findViewById(R.id.filter_editor_pattern_edittext);
        mModeSpinner = view.findViewById(R.id.filter_editor_mode_spinner);
        mCaseSensitiveSpinner = view.findViewById(R.id.filter_editor_case_spinner);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FilterEditorActivity activity = (FilterEditorActivity)requireActivity();

        // Set up spinner adapters
        mModeAdapter = new StringAdapter<>(
            getContext(), android.R.layout.simple_spinner_dropdown_item, getModeSpinnerItems());
        mModeSpinner.setAdapter(mModeAdapter);

        mCaseSensitiveAdapter = new StringAdapter<>(
            getContext(), android.R.layout.simple_spinner_dropdown_item, getCaseSensitiveSpinnerItems());
        mCaseSensitiveSpinner.setAdapter(mCaseSensitiveAdapter);

        // Load pattern data corresponding to the current tab
        mPatternData = activity.getPatternData(mField);

        // Disable hint animation as workaround for drawing issue during activity creation
        // See https://code.google.com/p/android/issues/detail?id=179776
        mPatternTextInputLayout.setHintAnimationEnabled(false);
        mPatternEditText.setText(mPatternData.getPattern());
        mPatternTextInputLayout.setHintAnimationEnabled(true);
        mPatternEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                mPatternData.setPattern(s.toString());
            }
        });

        mModeSpinner.setSelection(mModeAdapter.getPosition(mPatternData.getMode()));
        mModeSpinner.setOnItemSelectedListener(new OnItemSelectedListenerAdapter() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPatternData.setMode(mModeAdapter.getItem(position));
            }
        });

        mCaseSensitiveSpinner.setSelection(mCaseSensitiveAdapter.getPosition(mPatternData.isCaseSensitive()));
        mCaseSensitiveSpinner.setOnItemSelectedListener(new OnItemSelectedListenerAdapter() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPatternData.setCaseSensitive(mCaseSensitiveAdapter.getItem(position));
            }
        });
    }

    private LinkedHashMap<SmsFilterMode, String> getModeSpinnerItems() {
        Resources resources = getResources();
        LinkedHashMap<SmsFilterMode, String> modeMap = new LinkedHashMap<>();
        modeMap.put(SmsFilterMode.REGEX, resources.getString(R.string.filter_mode_regex));
        modeMap.put(SmsFilterMode.WILDCARD, resources.getString(R.string.filter_mode_wildcard));
        modeMap.put(SmsFilterMode.CONTAINS, resources.getString(R.string.filter_mode_contains));
        modeMap.put(SmsFilterMode.PREFIX, resources.getString(R.string.filter_mode_prefix));
        modeMap.put(SmsFilterMode.SUFFIX, resources.getString(R.string.filter_mode_suffix));
        modeMap.put(SmsFilterMode.EQUALS, resources.getString(R.string.filter_mode_equals));
        return modeMap;
    }

    private LinkedHashMap<Boolean, String> getCaseSensitiveSpinnerItems() {
        Resources resources = getResources();
        LinkedHashMap<Boolean, String> caseMap = new LinkedHashMap<>();
        caseMap.put(false, resources.getString(R.string.filter_case_insensitive));
        caseMap.put(true, resources.getString(R.string.filter_case_sensitive));
        return caseMap;
    }
}
