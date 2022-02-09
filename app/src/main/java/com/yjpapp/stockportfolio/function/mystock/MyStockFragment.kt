package com.yjpapp.stockportfolio.function.mystock

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.DatePicker
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yjpapp.stockportfolio.R
import com.yjpapp.stockportfolio.base.BaseFragment
import com.yjpapp.stockportfolio.databinding.FragmentMyStockBinding
import com.yjpapp.stockportfolio.dialog.CommonDatePickerDialog
import com.yjpapp.stockportfolio.extension.repeatOnStarted
import com.yjpapp.stockportfolio.function.mystock.dialog.MyStockInputDialog
import com.yjpapp.stockportfolio.localdb.room.mystock.MyStockEntity
import com.yjpapp.stockportfolio.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import es.dmoral.toasty.Toasty
import jp.wasabeef.recyclerview.animators.FadeInAnimator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*


/**
 * 나의 주식 화면
 * 디자인 패턴 : MVVM
 * @author Yoon Jae-park
 * @since 2021.04
 */
@AndroidEntryPoint
class MyStockFragment : BaseFragment<FragmentMyStockBinding>(R.layout.fragment_my_stock), MyStockAdapter.AdapterCallBack {

    private val myStockViewModel: MyStockViewModel by viewModels()
    private lateinit var myStockAdapter: MyStockAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myStockViewModel.onViewCreated()
        setHasOptionsMenu(true)
        initLayout()
        initData()
    }

    private var menu: Menu? = null
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_my_stock, menu)
        this.menu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_MyStockFragment_Add -> {
                myStockViewModel.apply {
                    inputDialogSubjectName = ""
                    inputDialogPurchaseDate = ""
                    inputDialogPurchasePrice.value = ""
                    inputDialogPurchaseCount = ""
                }
                showInputDialog(true, 0)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initLayout() {
        val tempMyStockList = mutableListOf<MyStockEntity>()
        myStockAdapter = MyStockAdapter(tempMyStockList)
        myStockViewModel.myStockInfoList.value?.let {
            myStockAdapter = MyStockAdapter(it)
        }
        myStockAdapter.setCallBack(this)
        binding.recyclerviewMyStockFragment.apply {
            layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false).apply {
                reverseLayout = true
                stackFromEnd = true
            }
            adapter = myStockAdapter
            itemAnimator = FadeInAnimator()
            addOnItemTouchListener(object :
                RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.actionMasked) {
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_DOWN -> {
                            myStockAdapter.closeSwipeLayout()
                        }
                    }
                    return false
                }
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }
    }

    private fun initData() {
        binding.apply {
            viewModel = myStockViewModel
            lifecycleOwner = this@MyStockFragment
        }
        //event handler
        lifecycleScope.launch {
            repeatOnStarted {
                myStockViewModel.eventFlow.collect { event -> handleEvent(event) }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleEvent(event: MyStockViewModel.Event) = when (event) {
        is MyStockViewModel.Event.ShowErrorToastMessage -> {
            Toasty.error(mContext, event.msg, Toast.LENGTH_SHORT).show()
        }
        is MyStockViewModel.Event.SendMyStockInfoList -> {
            val myStockInfoList = event.myStockInfoList
            myStockAdapter.setMyStockList(event.myStockInfoList)
            when (myStockViewModel.notifyHandler) {
                myStockViewModel.NOTIFY_HANDLER_INSERT -> {
                    myStockAdapter.notifyItemInserted(myStockInfoList.size - 1)
                    myStockAdapter.notifyItemRangeInserted(myStockInfoList.size - 1, myStockInfoList.size)
                    binding.recyclerviewMyStockFragment.scrollToPosition(myStockInfoList.size - 1)
                }
                myStockViewModel.NOTIFY_HANDLER_UPDATE -> {
                    myStockAdapter.notifyDataSetChanged()
                }
                myStockViewModel.NOTIFY_HANDLER_DELETE -> {
                    myStockAdapter.notifyItemRemoved(deletePosition)
                    myStockAdapter.notifyItemRangeRemoved(deletePosition, myStockInfoList.size)
                }
            }
            // 총 매수 금액 설정
            var mTotalPurchasePrice = 0.0
            event.myStockInfoList.forEach { myStockEntity ->
                mTotalPurchasePrice += Utils.getNumDeletedComma(myStockEntity.purchasePrice).toDouble()
            }
            myStockViewModel.totalPurchasePrice.value = NumberFormat.getCurrencyInstance(Locale.KOREA).format(mTotalPurchasePrice)

            //색상 설정
//                if (mTotalPurchasePrice >= 0) {
//                    binding.txtGainsLossesData.setTextColor(mContext.getColor(R.color.color_e52b4e))
//                    binding.txtGainPercentData.setTextColor(mContext.getColor(R.color.color_e52b4e))
//                } else {
//                    binding.txtGainsLossesData.setTextColor(mContext.getColor(R.color.color_4876c7))
//                    binding.txtGainPercentData.setTextColor(mContext.getColor(R.color.color_4876c7))
//                }
        }
    }

    private fun showInputDialog(isInsertMode: Boolean, id: Int) {
        MyStockInputDialog.getInstance(mContext).apply {
            myStockViewModel.inputDialogController = this
            binding.apply {
                viewModel = myStockViewModel
                etSubjectName.setOnClickListener {
//                    val intent = Intent(mContext, StockSearchActivity::class.java)
//                    startActivityForResult(intent, 10000)
                }
                etPurchaseDate.setOnClickListener {
                    var year = ""
                    var month = ""
                    if (binding.etPurchaseDate.text.toString() != "") {
                        val split = binding.etPurchaseDate.text.toString().split(".")
                        year = split[0]
                        month = split[1]
                    }
                    //매수 날짜 선택 다이얼로그 show
                    CommonDatePickerDialog(mContext, year, month).apply {
                        setListener { _: DatePicker?, year, month, dayOfMonth ->
                            uiHandler.sendEmptyMessage(MyStockInputDialog.MSG.SELL_DATE_DATA_INPUT)
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
                            myStockViewModel.inputDialogPurchaseDate =
                                "$purchaseYear.$purchaseMonth"
                        }
                        show()
                    }
                }
                txtCancel.setOnClickListener { dismiss() }
                txtComplete.setOnClickListener {
                    if (myStockViewModel.saveMyStock(mContext, isInsertMode, id)) {
                        dismiss()
                    }
                }

                //observer
                myStockViewModel.inputDialogPurchasePrice.observe(this@MyStockFragment, Observer {
                    etPurchasePrice.setText(it)
                    etPurchasePrice.setSelection(it.length)
                })

            }
            show()
        }
    }

    override fun onEditClick(myStockEntity: MyStockEntity?) {
        myStockViewModel.apply {
            if (myStockEntity == null) {
                Toasty.error(mContext, getString(R.string.Error_Msg_Not_Found_Data)).show()
                return
            }
            inputDialogSubjectName = myStockEntity.subjectName
            inputDialogPurchaseDate = myStockEntity.purchaseDate
            inputDialogPurchasePrice.value = myStockEntity.purchasePrice
            inputDialogPurchaseCount = myStockEntity.purchaseCount
            showInputDialog(false, myStockEntity.id)
        }
    }

    override fun onSellClick(myStockEntity: MyStockEntity?) {
        //TODO 수익 노트 연동.
    }

    var deletePosition = 0
    override fun onDeleteClick(myStockEntity: MyStockEntity?, position: Int) {
        deletePosition = position
        myStockEntity?.let {
            myStockViewModel.deleteMyStock(it)
        }
    }

}