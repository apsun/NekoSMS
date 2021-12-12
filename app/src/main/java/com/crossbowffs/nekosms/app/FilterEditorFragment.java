package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.utils.MapUtils;
import com.crossbowffs.nekosms.widget.EnumAdapter;
import com.crossbowffs.nekosms.widget.OnItemSelectedListenerAdapter;
import com.crossbowffs.nekosms.widget.TextWatcherAdapter;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

public class FilterEditorFragment extends Fragment {
    private enum CaseSensitivity {
        INSENSITIVE,
        SENSITIVE;

        public static CaseSensitivity fromBoolean(boolean caseSensitive) {
            return caseSensitive ? SENSITIVE : INSENSITIVE;
        }

        public boolean toBoolean() {
            return this == SENSITIVE;
        }
    }

    private TextInputLayout mPatternTextInputLayout;
    private EditText mPatternEditText;
    private Spinner mModeSpinner;
    private Spinner mCaseSpinner;
    private TextInputLayout mPatternTextInputLayout2;
    private EditText mPatternEditText2;
    private Spinner mModeSpinner2;
    private Spinner mCaseSpinner2;

    private RadioButton rdBtnBlackList;
    private RadioButton rdBtnWhiteList;
    private RadioGroup rdGroup;

    private EnumAdapter<SmsFilterMode> mModeAdapter;
    private EnumAdapter<CaseSensitivity> mCaseAdapter;

    private SmsFilterPatternData mPatternData;
    private SmsFilterPatternData mPatternData2;

    private FilterEditorActivity getEditorActivity() {
        return (FilterEditorActivity)getActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pattern_editor, container, false);
        mPatternTextInputLayout = (TextInputLayout)view.findViewById(R.id.filter_editor_pattern_inputlayout);
        mPatternEditText = (EditText)view.findViewById(R.id.filter_editor_pattern_edittext);
        mModeSpinner = (Spinner)view.findViewById(R.id.filter_editor_mode_spinner);
        mCaseSpinner = (Spinner)view.findViewById(R.id.filter_editor_case_spinner);

        mPatternTextInputLayout2 = (TextInputLayout)view.findViewById(R.id.filter_editor_pattern_inputlayout2);
        mPatternEditText2 = (EditText)view.findViewById(R.id.filter_editor_pattern_edittext2);
        mModeSpinner2 = (Spinner)view.findViewById(R.id.filter_editor_mode_spinner2);
        mCaseSpinner2 = (Spinner)view.findViewById(R.id.filter_editor_case_spinner2);

        rdGroup =view.findViewById(R.id.rd_group);
        rdBtnBlackList =view.findViewById(R.id.rdBtn_blacklist);
        rdBtnWhiteList =view.findViewById(R.id.rdBtn_whitelist);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up spinner adapters
        mModeAdapter = new EnumAdapter<>(getEditorActivity(), android.R.layout.simple_spinner_dropdown_item, SmsFilterMode.class);
        mModeAdapter.setStringMap(getModeMap());
        mModeSpinner.setAdapter(mModeAdapter);
        mModeSpinner2.setAdapter(mModeAdapter);

        mCaseAdapter = new EnumAdapter<>(getEditorActivity(), android.R.layout.simple_spinner_dropdown_item, CaseSensitivity.class);
        mCaseAdapter.setStringMap(getCaseMap());
        mCaseSpinner.setAdapter(mCaseAdapter);
        mCaseSpinner2.setAdapter(mCaseAdapter);

        // Load SenderPattern and BodyPattern
        mPatternData = getEditorActivity().getSmsFilterData().getSenderPattern();
        mPatternData2 = getEditorActivity().getSmsFilterData().getBodyPattern();

        //默认是黑名单，如果是白名单的话，勾选上白名单的按钮
        rdBtnWhiteList.setChecked(getEditorActivity().getSmsFilterData().getAction()==SmsFilterAction.ALLOW);
        rdGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==R.id.rdBtn_whitelist){
                    getEditorActivity().getSmsFilterData().setAction(SmsFilterAction.ALLOW);
                }else {
                    getEditorActivity().getSmsFilterData().setAction(SmsFilterAction.BLOCK);
                }
            }
        });

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

        mCaseSpinner.setSelection(mCaseAdapter.getPosition(CaseSensitivity.fromBoolean(mPatternData.isCaseSensitive())));
        mCaseSpinner.setOnItemSelectedListener(new OnItemSelectedListenerAdapter() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPatternData.setCaseSensitive(mCaseAdapter.getItem(position).toBoolean());
            }
        });

        // BODY——————————
        mPatternTextInputLayout2.setHintAnimationEnabled(false);
        mPatternEditText2.setText(mPatternData2.getPattern());
        mPatternTextInputLayout2.setHintAnimationEnabled(true);
        mPatternEditText2.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                mPatternData2.setPattern(s.toString());
            }
        });

        mModeSpinner2.setSelection(mModeAdapter.getPosition(mPatternData2.getMode()));
        mModeSpinner2.setOnItemSelectedListener(new OnItemSelectedListenerAdapter() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPatternData2.setMode(mModeAdapter.getItem(position));
            }
        });

        mCaseSpinner2.setSelection(mCaseAdapter.getPosition(CaseSensitivity.fromBoolean(mPatternData2.isCaseSensitive())));
        mCaseSpinner2.setOnItemSelectedListener(new OnItemSelectedListenerAdapter() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPatternData2.setCaseSensitive(mCaseAdapter.getItem(position).toBoolean());
            }
        });

        // 移动光标到合适的地方
        if (!mPatternData.hasData() && mPatternData2.hasData()) {
            mPatternEditText2.setSelection(mPatternEditText2.getText().length());
            mPatternEditText2.requestFocus();
        } else {
            mPatternEditText.setSelection(mPatternEditText.getText().length());
            mPatternEditText.requestFocus();
        }
    }

    private Map<SmsFilterMode, String> getModeMap() {
        Resources resources = getResources();
        HashMap<SmsFilterMode, String> modeMap = MapUtils.hashMapForSize(6);
        modeMap.put(SmsFilterMode.REGEX, resources.getString(R.string.filter_mode_regex));
        modeMap.put(SmsFilterMode.WILDCARD, resources.getString(R.string.filter_mode_wildcard));
        modeMap.put(SmsFilterMode.CONTAINS, resources.getString(R.string.filter_mode_contains));
        modeMap.put(SmsFilterMode.PREFIX, resources.getString(R.string.filter_mode_prefix));
        modeMap.put(SmsFilterMode.SUFFIX, resources.getString(R.string.filter_mode_suffix));
        modeMap.put(SmsFilterMode.EQUALS, resources.getString(R.string.filter_mode_equals));
        return modeMap;
    }

    private Map<CaseSensitivity, String> getCaseMap() {
        Resources resources = getResources();
        HashMap<CaseSensitivity, String> caseMap = MapUtils.hashMapForSize(2);
        caseMap.put(CaseSensitivity.INSENSITIVE, resources.getString(R.string.filter_case_insensitive));
        caseMap.put(CaseSensitivity.SENSITIVE, resources.getString(R.string.filter_case_sensitive));
        return caseMap;
    }
}
