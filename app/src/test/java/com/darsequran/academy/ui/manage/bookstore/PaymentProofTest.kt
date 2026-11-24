package com.darsequran.academy.ui.manage.bookstore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentProofTest {
    @Test
    fun pdfProofsAreToldApartByTheirName() {
        assertTrue(isPdfProof("/api/files/orders/o1/proof.pdf"))
        assertTrue(isPdfProof("https://files.example.test/uploads/PROOF.PDF"))
        assertTrue(isPdfProof("/api/files/orders/o1/proof.pdf?token=abc"))
        assertFalse(isPdfProof("/api/files/orders/o1/proof.jpg"))
        assertFalse(isPdfProof("/api/files/orders/o1/pdf-proof.png"))
    }
}
