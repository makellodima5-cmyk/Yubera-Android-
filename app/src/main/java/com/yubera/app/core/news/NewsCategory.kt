package com.yubera.app.core.news
enum class NewsCategory(val title:String){ALL("Все"),ANNOUNCEMENTS("Анонсы"),NEW_SEASONS("Новые сезоны"),INDUSTRY("Индустрия"),EVENTS("События"),INTERVIEWS("Интервью");
 companion object{fun classify(title:String,summary:String):NewsCategory{val text="$title $summary".lowercase();return when{
 listOf("интервью","interview","разговор с").any(text::contains)->INTERVIEWS
 listOf("второй сезон","новый сезон","продолжение","season 2","new season","sequel","сезон 2","season 3","сезон 3").any(text::contains)->NEW_SEASONS
 listOf("анонс","тизер","трейлер","announcement","announced","trailer","teaser","premiere date","дата премьеры").any(text::contains)->ANNOUNCEMENTS
 listOf("фестивал","выстав","преми","наград","конвент","festival","award","convention","event","мероприят").any(text::contains)->EVENTS
 listOf("студия","производств","режиссёр","режиссер","сценарист","анимац","industry","production","staff","кадров").any(text::contains)->INDUSTRY
 else->ALL
}}}}