package com.yjpapp.stockportfolio.database.model

data class MyStockInfo
    (var id: Int,
     var subjectName: String?, // 종목명
     var realPainLossesAmount: String?, //순손익금액
     var purchaseDate: String?, // 매수일
     var gainPercent: String?, // 수익률
     var purchasePrice: String?, // 매수단가
     var currentPrice: String?, // 현재가
     var holdingQuantity: String? // 보유 수량
     )