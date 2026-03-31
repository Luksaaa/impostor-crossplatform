package com.example.impostergame

import dev.gitlive.firebase.database.DataSnapshot

inline fun <reified T> DataSnapshot.getValueSafe(): T = this.value()
