This is my HABContentMoleculeView.kt
open class HABContentMoleculeView : RelativeLayout, View.OnFocusChangeListener,
    StyleApplier<HABContentModel> {

    @set:Inject
    internal var presenter: BasePresenter? = null

    @set:Inject
    internal var globalSearchViewPresenter: GlobalSearchViewPresenter? = null

    @set:Inject
    internal var sharedPreferencesUtil: SharedPreferencesUtil? = null
    var habClickListener: OnHabClickListener? = null
    var iv_hab_back: ImageAtomView? = null
    var iv_search: ImageAtomView? = null
    var et_search: LabelAtomView? = null
    var divider: RelativeLayout? = null
    var iv_assistant: ImageAtomView? = null
    var hab_content_view_root: LinearLayout? = null
    var rootRl: RelativeLayout? = null
    var searchArea: RelativeLayout? = null
    var searchResultsView: SearchTextFieldView? = null
    var searchAction: ActionModel? = null
    var searchDoneAction: ActionModel? = null
    var searchJob: Job? = null
    var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    var onSearchListener: OnSearchChangeListener? = null
    var isSearchActive = false
    var inverted = false
    var globalSearchListTemplateModel: GlobalSearchListTemplateModel? = null
    var baseActivity: BaseActivity? = null
    var initialialMoleculeList: List<DelegateModel>? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initView(context)
    }

    fun initializeActivity(baseActivity: BaseActivity) {
        this.baseActivity = baseActivity
    }


    private fun initView(context: Context) {
        View.inflate(context, R.layout.hab_content_view, this)
        MobileFirstApplication.getObjectGraph(context.applicationContext).inject(this)
        iv_search = findViewById(R.id.iv_search)
        et_search = findViewById(R.id.et_search)
        iv_hab_back = findViewById(R.id.iv_hab_back)
        divider = findViewById(R.id.dividerContainer)
        iv_assistant = findViewById(R.id.iv_assistant)
        hab_content_view_root = findViewById(R.id.hab_content_view_root)
        rootRl = findViewById(R.id.rootRl)
        searchArea = findViewById(R.id.searchArea)
        searchResultsView = findViewById(R.id.searchResultsView)
        searchResultsView?.onFocusChangeListener = this
        searchResultsView?.isFocusableInTouchMode = true //Do not keep focus unless user taps on it
        searchResultsView?.isFocusable = true
        imm.showSoftInput(searchResultsView, InputMethodManager.SHOW_IMPLICIT)
        searchResultsView?.showSoftInputOnFocus
        observerSearch()

        searchResultsView?.setOnEditorActionListener { view, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = searchResultsView?.text.toString()
                performDoneApiCall(query)
                true
            } else false
        }
        searchResultsView?.imeOptions = EditorInfo.IME_ACTION_SEARCH
        iv_hab_back?.setOnClickListener {
            onSearchListener?.onHabBackPressed()
        }
    }


    private fun executeAction(actionModel: ActionModel) {
        val action = convertToAction(actionModel)
        habClickListener?.OnHabActionClick(action)
    }

    open fun initListener(onHabClickListener: OnHabClickListener) {
        this.habClickListener = onHabClickListener
    }

    override fun applyStyle(model: HABContentModel) {
        applyCommonStyles(model)
        this.visibility = View.VISIBLE
        rootRl?.contentDescription = null
        iv_assistant?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        searchArea?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        et_search?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        searchAction = model.searchActionModel
        searchDoneAction = model.searchDoneActionModel
        inverted = model.inverted
        globalSearchListTemplateModel =
            model.globalSearchListTemplateModel as? GlobalSearchListTemplateModel
        initialialMoleculeList = model.sectionMoleculeList
        if (model.quertHintLabel != null) {
            searchResultsView?.hint = model.quertHintLabel?.text
            if (model.quertHintLabel?.fontStyle == null) {
                searchResultsView?.typeface =
                    Utils.getFontStyle(context, Utils.VERIZONNHGEDS_REGULAR)
            } else {
                searchResultsView?.typeface =
                    Utils.getFontStyle(context, model.quertHintLabel?.fontStyle)
            }
        }
        if (model.inverted) {
            //dark background
            rootRl?.let {
                rootRl?.setBackgroundDrawable(it.context.resources.getDrawable(R.drawable.background_hab_dark))
                searchResultsView?.setHintTextColor(Color.LTGRAY)
                searchResultsView?.setTextColor(Color.WHITE)
                searchResultsView?.setBackgroundColor(Color.TRANSPARENT)
            }
            if (model.configuration == "dual" && model.searchActionModel != null) {
                iv_search?.visibility = View.VISIBLE
                et_search?.visibility = View.GONE
                model.habSearch?.image_inverted?.let {
                    iv_search?.setOnClickListener { }
                    iv_search?.applyStyle(it)
                }
                divider?.visibility = VISIBLE
                searchResultsView?.visibility = VISIBLE
                iv_assistant?.visibility = VISIBLE
            } else if (model.configuration == "dual" && model.habSearch?.image_inverted != null) {
                searchResultsView?.visibility = GONE
                iv_search?.visibility = VISIBLE
                model.habSearch?.image_inverted?.let {
                    iv_search?.applyStyle(it)
                }
                searchArea?.setOnClickListener {
                    model.habSearch?.action?.let { it1 -> executeAction(it1) }
                }
                model.habSearch?.label?.let {
                    et_search?.applyStyle(it)
                }
                divider?.visibility = VISIBLE
            } else {
                iv_search?.visibility = GONE
                searchResultsView?.visibility = GONE
            }


            if (model.habContentState == HABContentAtomEnum.ASSISTANT && model.habAssistant?.image_inverted != null) {
                model.habAssistant?.image_inverted?.let {
                    iv_assistant?.applyStyle(it)
                }
                iv_assistant?.setOnClickListener {
                    model.habAssistant?.action?.let { it1 -> executeAction(it1) }
                }
                if (model.configuration.equals("single")) {
                    model.habAssistant?.label?.let {
                        et_search?.applyStyle(it)
                    }
                    divider?.visibility = GONE
                    searchArea?.setOnClickListener {
                        model.habAssistant?.action?.let { it1 -> executeAction(it1) }
                    }
                    rootRl?.contentDescription = model.habAssistant?.label?.accessibilityText
                    iv_assistant?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    searchArea?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    et_search?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            } else if (model.habContentState == HABContentAtomEnum.LIVECHAT && model.habLiveChat?.image_inverted != null) {
                model.habLiveChat?.image_inverted?.let {
                    iv_assistant?.applyStyle(it)
                }
                iv_assistant?.setOnClickListener {
                    model.habLiveChat?.action?.let { it1 -> executeAction(it1) }
                }
                if (model.configuration.equals("single")) {
                    divider?.visibility = GONE
                    model.habLiveChat?.label?.let {
                        et_search?.applyStyle(it)
                    }
                    searchArea?.setOnClickListener {
                        model.habLiveChat?.action?.let { it1 -> executeAction(it1) }
                    }
                    rootRl?.contentDescription = model.habLiveChat?.label?.accessibilityText
                    iv_assistant?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    searchArea?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    et_search?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            } else if (model.habContentState == HABContentAtomEnum.LIVE_CHAT_NEW_MESSAGE && model.habLiveChatNewMessages?.image_inverted != null) {
                model.habLiveChatNewMessages?.image_inverted?.let {
                    iv_assistant?.applyStyle(it)
                }
                iv_assistant?.setOnClickListener {
                    model.habLiveChatNewMessages?.action?.let { it1 -> executeAction(it1) }
                }
                if (model.configuration.equals("single")) {
                    divider?.visibility = GONE
                    model.habLiveChatNewMessages?.label?.let {
                        et_search?.applyStyle(it)
                    }
                    searchArea?.setOnClickListener {
                        model.habLiveChatNewMessages?.action?.let { it1 -> executeAction(it1) }
                    }
                    rootRl?.contentDescription =
                        model.habLiveChatNewMessages?.label?.accessibilityText
                    iv_assistant?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    searchArea?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    et_search?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            }

            rootRl?.context?.resources?.getColor(R.color.white)?.let { et_search?.setTextColor(it) }
        } else {
            //Light background
            rootRl?.setBackgroundDrawable(rootRl?.context?.resources?.getDrawable(R.drawable.background_hab_light))
            searchResultsView?.setBackgroundColor(Color.WHITE)
            searchResultsView?.setHintTextColor(Color.LTGRAY)
            searchResultsView?.setTextColor(Color.BLACK)
//            searchView?.setBackgroundColor(Color.WHITE)
            if (model.configuration.equals("dual") && model.searchActionModel != null) {
                iv_search?.visibility = View.VISIBLE
                et_search?.visibility = View.GONE
                model.habSearch?.image?.let {
                    iv_search?.applyStyle(it)
                    iv_search?.setOnClickListener { }
                }
                divider?.visibility = VISIBLE
                searchResultsView?.visibility = VISIBLE
                iv_assistant?.visibility = VISIBLE
            } else if (model.configuration != null && model.configuration.equals("dual") && model.habSearch?.image != null) {
                searchResultsView?.visibility = GONE
                iv_search?.visibility = VISIBLE
                model.habSearch?.image?.let {
                    iv_search?.applyStyle(it)
                }
                searchArea?.setOnClickListener {
                    model.habSearch?.action?.let { it1 -> executeAction(it1) }
                }
                divider?.visibility = VISIBLE
                model.habSearch?.label?.let {
                    et_search?.applyStyle(it)
                }
            } else {
                iv_search?.visibility = GONE
                searchResultsView?.visibility = GONE
            }
            if (model.habContentState == HABContentAtomEnum.ASSISTANT && model.habAssistant?.image != null) {
                model.habAssistant?.image?.let {
                    iv_assistant?.applyStyle(it)
                }
                iv_assistant?.setOnClickListener {
                    model.habAssistant?.action?.let { it1 -> executeAction(it1) }
                }
                if (model.configuration.equals("single")) {
                    divider?.visibility = GONE
                    model.habAssistant?.label?.let {
                        et_search?.applyStyle(it)
                    }
                    searchArea?.setOnClickListener {
                        model.habAssistant?.action?.let { it1 -> executeAction(it1) }
                    }
                    if (model.habAssistant?.label?.accessibilityText?.isNotEmpty() == true) {
                        rootRl?.contentDescription = model.habAssistant?.label?.accessibilityText
                    } else {
                        rootRl?.contentDescription = model.habAssistant?.label?.text
                    }
                    iv_assistant?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    searchArea?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    et_search?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                if (model.habAssistant?.label?.accessibilityText?.isNotEmpty() == true) {
                    iv_assistant?.contentDescription = model.habAssistant?.label?.accessibilityText
                } else {
                    iv_assistant?.contentDescription = model.habAssistant?.label?.text
                }
            } else if (model.habContentState == HABContentAtomEnum.LIVECHAT && model.habLiveChat?.image != null) {
                model.habLiveChat?.image?.let {
                    iv_assistant?.applyStyle(it)
                }
                iv_assistant?.setOnClickListener {
                    model.habLiveChat?.action?.let { it1 -> executeAction(it1) }
                }
                if (model.configuration.equals("single")) {
                    divider?.visibility = GONE
                    model.habLiveChat?.label?.let {
                        et_search?.applyStyle(it)
                    }
                    searchArea?.setOnClickListener {
                        model.habLiveChat?.action?.let { it1 -> executeAction(it1) }
                    }
                    if (model.habLiveChat?.label?.accessibilityText?.isNotEmpty() == true) {
                        rootRl?.contentDescription = model.habLiveChat?.label?.accessibilityText
                    } else rootRl?.contentDescription = model.habLiveChat?.label?.text
                    iv_assistant?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    searchArea?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    et_search?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                if (model.habLiveChat?.label?.accessibilityText?.isNotEmpty() == true) {
                    iv_assistant?.contentDescription = model.habLiveChat?.label?.accessibilityText
                } else iv_assistant?.contentDescription = model.habLiveChat?.label?.text
            } else if (model.habContentState == HABContentAtomEnum.LIVE_CHAT_NEW_MESSAGE && model.habLiveChatNewMessages?.image != null) {
                model.habLiveChatNewMessages?.image?.let {
                    iv_assistant?.applyStyle(it)
                }
                iv_assistant?.setOnClickListener {
                    model.habLiveChatNewMessages?.action?.let { it1 -> executeAction(it1) }
                }
                if (model.configuration.equals("single")) {
                    divider?.visibility = GONE
                    model.habLiveChatNewMessages?.label?.let {
                        et_search?.applyStyle(it)
                    }
                    searchArea?.setOnClickListener {
                        model.habLiveChatNewMessages?.action?.let { it1 -> executeAction(it1) }
                    }
                    if (model.habLiveChatNewMessages?.label?.accessibilityText?.isNotEmpty() == true) {
                        rootRl?.contentDescription =
                            model.habLiveChatNewMessages?.label?.accessibilityText
                    } else rootRl?.contentDescription = model.habLiveChatNewMessages?.label?.text
                    iv_assistant?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    searchArea?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    et_search?.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                if (model.habLiveChatNewMessages?.label?.accessibilityText?.isNotEmpty() == true) {
                    iv_assistant?.contentDescription =
                        model.habLiveChatNewMessages?.label?.accessibilityText
                } else iv_assistant?.contentDescription = model.habLiveChatNewMessages?.label?.text
            }
            rootRl?.context?.resources?.getColor(R.color.black)?.let { et_search?.setTextColor(it) }
        }
    }

    fun habSearchListener(onSearchChangeListener: OnSearchChangeListener) {
        onSearchListener = onSearchChangeListener
    }

    override fun onFocusChange(p0: View?, focus: Boolean) {
        if (focus) {
            iv_hab_back?.visibility = View.VISIBLE
            iv_assistant?.visibility = View.INVISIBLE
            divider?.visibility = GONE
            if (inverted) {
                hab_content_view_root?.setBackgroundColor(
                    ContextCompat.getColor(
                        this.context,
                        R.color.black
                    )
                )
                iv_hab_back?.setColorFilter(ContextCompat.getColor(this.context, R.color.white))
            } else {
                hab_content_view_root?.setBackgroundColor(
                    ContextCompat.getColor(
                        this.context,
                        R.color.white
                    )
                )
                iv_hab_back?.setColorFilter(ContextCompat.getColor(this.context, R.color.black))
            }
            if (!scope.isActive) {
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                observerSearch()
            }
            isSearchActive = true
            onSearchListener?.showOrHideOverlay(
                true,
                globalSearchListTemplateModel = initialSearchTemplate()
            )
            searchResultsView?.text?.toString()?.let {
                if (it.isNotEmpty()) {
                    performApiCall(it)
                }
            }
        }
    }

    fun clearSearchFocus() {
        searchResultsView?.clearFocus()
        searchResultsView?.setText(Constants.EMPTY)
        searchResultsView?.hideKeyboard()
    }

    fun showFabIcon() {
        iv_assistant?.visibility = View.VISIBLE
    }

    fun hideBackIcon() {
        iv_hab_back?.visibility = View.GONE
        divider?.visibility = VISIBLE
        isSearchActive = false
        hab_content_view_root?.setBackgroundColor(Color.TRANSPARENT)
    }
