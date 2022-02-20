package ren.imyan.app_tracker

sealed class FilterAppType {
    object User : FilterAppType()
    object System : FilterAppType()
    object All : FilterAppType()
}
