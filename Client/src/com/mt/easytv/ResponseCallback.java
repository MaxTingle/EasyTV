package com.mt.easytv;

import uk.co.maxtingle.communication.common.Message;

public interface ResponseCallback
{
    void onResponse(Message reply) throws Exception;
}