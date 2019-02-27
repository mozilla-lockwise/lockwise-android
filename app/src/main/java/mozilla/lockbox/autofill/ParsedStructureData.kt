package mozilla.lockbox.autofill

import android.os.Build
import android.os.Parcel
import android.os.ParcelFormatException
import android.os.Parcelable
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi

open class ParsedStructureData<Id>(
    val usernameId: Id? = null,
    val passwordId: Id? = null,
    val webDomain: String? = null,
    val packageName: String
)

@RequiresApi(Build.VERSION_CODES.O)
class ParsedStructure(
    usernameId: AutofillId? = null,
    passwordId: AutofillId? = null,
    webDomain: String? = null,
    packageName: String
) : ParsedStructureData<AutofillId>(usernameId, passwordId, webDomain, packageName), Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(AutofillId::class.java.classLoader),
        parcel.readParcelable(AutofillId::class.java.classLoader),
        parcel.readString(),
        parcel.readString() ?: throw ParcelFormatException("packageName")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(this.usernameId, flags)
        parcel.writeParcelable(this.passwordId, flags)
        parcel.writeString(this.webDomain)
        parcel.writeString(this.packageName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParsedStructure> {
        override fun createFromParcel(parcel: Parcel): ParsedStructure {
            return ParsedStructure(parcel)
        }

        override fun newArray(size: Int): Array<ParsedStructure?> {
            return arrayOfNulls(size)
        }
    }
}