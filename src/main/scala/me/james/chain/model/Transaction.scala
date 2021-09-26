package me.james.chain.model

case class Transaction(
    sender: String,
    recipient: String,
    value: Long
)
