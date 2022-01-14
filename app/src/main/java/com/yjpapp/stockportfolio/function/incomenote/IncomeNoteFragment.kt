package com.yjpapp.stockportfolio.function.incomenote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yjpapp.stockportfolio.R
import com.yjpapp.stockportfolio.base.BaseFragment
import com.yjpapp.stockportfolio.constance.StockConfig
import com.yjpapp.stockportfolio.databinding.FragmentIncomeNoteBinding
import com.yjpapp.stockportfolio.dialog.CommonDatePickerDialog
import com.yjpapp.stockportfolio.dialog.CommonTwoBtnDialog
import com.yjpapp.stockportfolio.extension.OnSingleClickListener
import com.yjpapp.stockportfolio.extension.repeatOnStarted
import com.yjpapp.stockportfolio.function.incomenote.dialog.IncomeNoteDatePickerDialog
import com.yjpapp.stockportfolio.function.incomenote.dialog.IncomeNoteInputDialog
import com.yjpapp.stockportfolio.function.memo.MemoListFragment
import com.yjpapp.stockportfolio.localdb.preference.PrefKey
import com.yjpapp.stockportfolio.localdb.preference.PreferenceController
import com.yjpapp.stockportfolio.model.request.ReqIncomeNoteInfo
import com.yjpapp.stockportfolio.model.response.RespIncomeNoteListInfo
import com.yjpapp.stockportfolio.network.ResponseAlertManger
import com.yjpapp.stockportfolio.util.Utils
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.math.BigDecimal
import java.util.*

/**
 * 수익노트 화면
 * 디자인 패턴 : MVP
 * @author Yoon Jae-park
 * @since 2020.08
 */
class IncomeNoteFragment : BaseFragment<FragmentIncomeNoteBinding>(R.layout.fragment_income_note) {
    private val TAG = IncomeNoteFragment::class.java.simpleName
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var incomeNoteListAdapter = IncomeNoteListAdapter(arrayListOf(), null).apply { setHasStableIds(true) }

    private val viewModel: IncomeNoteViewModel by inject()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //Fragment BackPress Event Call
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.let {
                    Utils.runBackPressAppCloseEvent(mContext, it)
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
        initData()
    }

    override fun onResume() {
        super.onResume()
        activity?.invalidateOptionsMenu()
    }

    override fun onDetach() {
        super.onDetach()
        onBackPressedCallback.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.unbind()
    }

    private var menu: Menu? = null
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_income_note, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_IncomeNoteFragment_Add -> {
                viewModel.editMode = false
                viewModel.incomeNoteId = -1
                showInputDialog(viewModel.editMode, null)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initLayout() {
        setHasOptionsMenu(true)
        incomeNoteListAdapter.callBack = adapterCallBack
        binding.apply {
            btnDate.setOnClickListener(onClickListener)
            btnSearchAll.setOnClickListener(onClickListener)
        }

        initRecyclerView()
    }

    private val onClickListener = OnSingleClickListener { view: View? ->
        when (view?.id) {
            R.id.lin_MainActivity_BottomMenu_Memo -> {
                val intent = Intent(mContext, MemoListFragment::class.java)
                startActivity(intent)
            }

            R.id.btn_date -> {
                IncomeNoteDatePickerDialog(
                    datePickerDialogCallBack,
                    viewModel.initStartYYYYMMDD,
                    viewModel.initEndYYYYMMDD
                ).apply {
                    show(this@IncomeNoteFragment.childFragmentManager, TAG)
                }
            }

            R.id.btn_search_all -> {
                incomeNoteListAdapter.incomeNoteListInfo = arrayListOf()
                viewModel.apply {
                    initStartYYYYMMDD = listOf()
                    initEndYYYYMMDD = listOf()
                    page = 1
                    requestGetIncomeNote(requireContext())
                    requestTotalGain(requireContext())
                    setDateText()
                }
            }
        }
    }

    private fun initRecyclerView() {
        binding.recyclerviewIncomeNoteFragment.apply {
            layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
            adapter = incomeNoteListAdapter
            addOnScrollListener(onScrollListener)
            addOnItemTouchListener(object: RecyclerView.OnItemTouchListener{
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when(e.actionMasked){
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_DOWN -> {
                            incomeNoteListAdapter.closeSwipeLayout()
                        }
                    }
                    return false
                }
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }
    }

    fun showAddButton() {
        menu?.getItem(0)?.isVisible = true
    }

    fun hideAddButton() {
        menu?.getItem(0)?.isVisible = false
    }

    fun showInputDialog(editMode: Boolean, respIncomeNoteInfoInfo: RespIncomeNoteListInfo.IncomeNoteInfo?) {
        IncomeNoteInputDialog(inputDialogCallBack, mContext).apply {
            if(editMode){
                if (!isShowing) {
                    show()
                    respIncomeNoteInfoInfo?.let {
                        etSubjectName.setText(it.subjectName)
                        etSellDate.setText(it.sellDate)
                        etPurchasePrice.setText(Utils.getNumInsertComma(BigDecimal(it.purchasePrice).toString()))
                        etSellPrice.setText(Utils.getNumInsertComma(BigDecimal(it.sellPrice).toString()))
                        etSellCount.setText(it.sellCount.toString())
                    }
                }
            }else{
                if (!isShowing) {
                    show()
                }
            }

            etSellDate.setOnClickListener {
                var year = ""
                var month = ""
                var day = ""
                if(etSellDate.text.toString() != "") {
                    val split = etSellDate.text.toString().split("-")
                    year = split[0]
                    month = split[1]
                    day = split[2]
                }

                CommonDatePickerDialog(mContext, year, month, day).apply {
                    setListener { _: DatePicker?, year, month, dayOfMonth ->
//                        Toast.makeText(requireContext(), "날짜 : $year/$month/$dayOfMonth", Toast.LENGTH_LONG).show()
                        uiHandler.sendEmptyMessage(IncomeNoteInputDialog.MSG.PURCHASE_DATE_DATA_INPUT)
                        purchaseYear = year.toString()
                        purchaseMonth = if (month < 10) {
                            "0$month"
                        } else {
                            month.toString()
                        }
                        purchaseDay = if (dayOfMonth < 10) {
                            "0$dayOfMonth"
                        } else {
                            dayOfMonth.toString()
                        }
                    }
                    show()
                }
            }
        }
    }

    private fun initData() {
        viewModel.apply {
            page = 1
            requestGetIncomeNote(mContext)
            requestTotalGain(mContext)
            setDateText()
        }
        //event handler
        lifecycleScope.launch {
            repeatOnStarted {
                viewModel.eventFlow.collect { event -> handleEvent(event) }
            }
        }
    }

    private val adapterCallBack = object : IncomeNoteListAdapter.CallBack {
        override fun onEditButtonClick(respIncomeNoteListInfo: RespIncomeNoteListInfo.IncomeNoteInfo?) {
            respIncomeNoteListInfo?.let {
                viewModel.editMode = true
                viewModel.incomeNoteId = it.id
                showInputDialog(viewModel.editMode, respIncomeNoteListInfo)
            }
        }

        override fun onDeleteButtonClick(respIncomeNoteListInfo: RespIncomeNoteListInfo.IncomeNoteInfo?, position: Int) {
            if (respIncomeNoteListInfo != null) {
                val isShowDeleteCheck = PreferenceController.getInstance(mContext)
                    .getPreference(PrefKey.KEY_SETTING_INCOME_NOTE_SHOW_DELETE_CHECK) ?: StockConfig.TRUE
                if (isShowDeleteCheck == StockConfig.TRUE) {
                    CommonTwoBtnDialog(
                        mContext,
                        CommonTwoBtnDialog.CommonTwoBtnData(
                            noticeText = "삭제하시겠습니까?",
                            leftBtnText = mContext.getString(R.string.Common_Cancel),
                            rightBtnText = mContext.getString(R.string.Common_Ok),
                            leftBtnListener = object : CommonTwoBtnDialog.OnClickListener {
                                override fun onClick(view: View, dialog: CommonTwoBtnDialog) {
                                    dialog.dismiss()
                                }
                            },
                            rightBtnListener = object : CommonTwoBtnDialog.OnClickListener {
                                override fun onClick(view: View, dialog: CommonTwoBtnDialog) {
                                    if (incomeNoteListAdapter.itemCount > position) {
                                        viewModel.requestDeleteIncomeNote(mContext, respIncomeNoteListInfo.id, position)
                                        dialog.dismiss()
                                    }
                                }
                            }
                        )
                    ).show()
                } else {
                    viewModel.requestDeleteIncomeNote(mContext, respIncomeNoteListInfo.id, position)
                }
            }
        }
    }

    private val datePickerDialogCallBack = object : IncomeNoteDatePickerDialog.CallBack {
        @SuppressLint("NotifyDataSetChanged")
        override fun requestIncomeNoteList(startDateList: List<String>, endDateList: List<String>) {
            binding.btnDate
            lifecycleScope.launch {
                incomeNoteListAdapter.incomeNoteListInfo = arrayListOf()
                incomeNoteListAdapter.notifyDataSetChanged()
                viewModel.apply {
                    initStartYYYYMMDD = startDateList
                    initEndYYYYMMDD = endDateList
                    page = 1
                    requestGetIncomeNote(mContext)
                    requestTotalGain(mContext)
                    setDateText()
                }
            }
        }
    }

    private val inputDialogCallBack = object : IncomeNoteInputDialog.CallBack {
        override fun onInputDialogCompleteClicked(reqIncomeNoteInfo: ReqIncomeNoteInfo) {
            reqIncomeNoteInfo.id = viewModel.incomeNoteId
            if (viewModel.editMode) {
                viewModel.requestModifyIncomeNote(mContext, reqIncomeNoteInfo)
            } else {
                viewModel.requestAddIncomeNote(mContext, reqIncomeNoteInfo)
            }
        }
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (viewModel.hasNext) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
                if (lastVisible >= totalItemCount - 1) {
                    viewModel.page++
                    viewModel.requestGetIncomeNote(mContext)
                }
            }
        }
    }

    private fun setDateText() {
        viewModel.run {
            if (initEndYYYYMMDD.isNotEmpty() && initEndYYYYMMDD.isNotEmpty()) {
                val startDate = makeDateString(initStartYYYYMMDD)
                val endDate = makeDateString(initEndYYYYMMDD)
                binding.txtFilterDate.text = "$startDate ~ $endDate"
            } else {
                binding.txtFilterDate.text = getString(R.string.Common_All)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleEvent(event: IncomeNoteViewModel.Event) = when (event) {
        is IncomeNoteViewModel.Event.SendTotalGainData -> {
            val totalGainNumber = event.data.total_price
            val totalGainPercent = event.data.total_percent
            binding.let {
                val totalRealizationGainsLossesNumber = Utils.getNumInsertComma(BigDecimal(totalGainNumber).toString())
                it.txtTotalRealizationGainsLossesData.text = "${StockConfig.moneySymbol}$totalRealizationGainsLossesNumber"
                if (totalGainPercent >= 0) {
                    it.txtTotalRealizationGainsLossesData.setTextColor(mContext.getColor(R.color.color_e52b4e))
                    it.txtTotalRealizationGainsLossesPercent.setTextColor(mContext.getColor(R.color.color_e52b4e))
                } else {
                    it.txtTotalRealizationGainsLossesData.setTextColor(mContext.getColor(R.color.color_4876c7))
                    it.txtTotalRealizationGainsLossesPercent.setTextColor(mContext.getColor(R.color.color_4876c7))
                }
                it.txtTotalRealizationGainsLossesPercent.text = Utils.getRoundsPercentNumber(totalGainPercent)
            }
        }
        is IncomeNoteViewModel.Event.IncomeNoteDeleteSuccess -> {
            Toasty.normal(mContext, "삭제완료").show()
            incomeNoteListAdapter.incomeNoteListInfo.removeAt(event.position)
            incomeNoteListAdapter.notifyItemRemoved(event.position)
            incomeNoteListAdapter.notifyDataSetChanged()
            viewModel.requestTotalGain(mContext)
        }
        is IncomeNoteViewModel.Event.IncomeNoteAddSuccess -> {
            Toasty.info(mContext, "추가완료").show()
            incomeNoteListAdapter.incomeNoteListInfo.add(0, event.data)
//                incomeNoteListAdapter.notifyItemInserted(incomeNoteListAdapter.itemCount - 1)
            incomeNoteListAdapter.notifyDataSetChanged()
            viewModel.requestTotalGain(mContext)
        }
        is IncomeNoteViewModel.Event.IncomeNoteModifySuccess -> {
            Toasty.normal(mContext, "수정완료").show()
            viewModel.requestTotalGain(mContext)
            if (event.data.id != -1) {
                val beforeModifyIncomeNote = incomeNoteListAdapter.incomeNoteListInfo.find { it.id == event.data.id }
                val index = incomeNoteListAdapter.incomeNoteListInfo.indexOf(beforeModifyIncomeNote)
                incomeNoteListAdapter.incomeNoteListInfo[index] = event.data
                incomeNoteListAdapter.notifyDataSetChanged()
                viewModel.requestTotalGain(mContext)
            } else {
                ResponseAlertManger.showErrorAlert(
                    mContext,
                    getString(R.string.Error_Msg_Normal)
                )
            }
        }
        is IncomeNoteViewModel.Event.FetchUIncomeNotes -> {
            event.data.forEach {
                incomeNoteListAdapter.incomeNoteListInfo.add(it)
            }
            incomeNoteListAdapter.notifyDataSetChanged()
        }
    }
}