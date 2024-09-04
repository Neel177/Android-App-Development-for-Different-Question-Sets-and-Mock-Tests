package com.example.qbite

import java.io.Serializable

data class QuizModel(
    val id : String,
    val title :String,
    val subtitle :String,
    val time : String,
    val questionList: List<QuestionModel>,
    val category: String
){
    constructor() : this("","","","", emptyList(),"")
}

data class QuestionModel(
    val id: String,
    val question : String,
    val options : List<String>,
    val correct: String,

): Serializable {
    constructor(): this ( "", "",emptyList(),"")
}
